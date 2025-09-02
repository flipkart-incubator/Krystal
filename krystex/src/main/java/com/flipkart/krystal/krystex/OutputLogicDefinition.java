package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.LogicDecoratorContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public abstract sealed class OutputLogicDefinition<T> extends LogicDefinition<OutputLogic<T>>
    permits IOLogicDefinition, ComputeLogicDefinition {

  private @MonotonicNonNull Boolean decoratorsNeedFlushing;

  @Getter
  private ImmutableMap<String, List<OutputLogicDecoratorConfig>>
      requestScopedLogicDecoratorConfigs = ImmutableMap.of();

  /** LogicDecorator Id -> LogicDecorator */
  private final Map<String, OutputLogicDecoratorConfig> sessionScopedLogicDecoratorConfigs =
      new ConcurrentHashMap<>();

  private final Map<String, ConcurrentHashMap<String, OutputLogicDecorator>> sessionScopedDecorators =
      new ConcurrentHashMap<>();

  private final Map<KryonId, ImmutableMap<String, OutputLogicDecorator>>
      sessionScopedDecoratorsByKryon = new HashMap<>();

  protected OutputLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<String> inputs,
      ImmutableMap<Object, Tag> logicTags,
      OutputLogic<T> outputLogic) {
    super(kryonLogicId, inputs, logicTags, outputLogic);
  }

  public final ImmutableMap<Facets, CompletableFuture<@Nullable T>> execute(
      ImmutableList<Facets> inputs) {
    return logic().execute(inputs);
  }

  public ImmutableMap<String, OutputLogicDecorator> getSessionScopedLogicDecorators(
      KryonDefinition kryonDefinition, DependantChain dependants) {
    return sessionScopedDecoratorsByKryon.computeIfAbsent(
        kryonDefinition.kryonId(),
        _k -> {
          Map<String, OutputLogicDecorator> decorators = new LinkedHashMap<>();
          sessionScopedLogicDecoratorConfigs.forEach(
              (s, decoratorConfig) -> {
                try {
                  LogicExecutionContext logicExecutionContext =
                      new LogicExecutionContext(
                          kryonDefinition.kryonId(),
                          logicTags(),
                          dependants,
                          kryonDefinition.kryonDefinitionRegistry());
                  String instanceId =
                      decoratorConfig.instanceIdGenerator().apply(logicExecutionContext);

                  if (decoratorConfig.shouldDecorate().test(logicExecutionContext)) {
                    decorators.put(
                        s,
                        sessionScopedDecorators
                            .computeIfAbsent(s, k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(
                                instanceId,
                                k ->
                                    decoratorConfig
                                        .factory()
                                        .apply(
                                            new LogicDecoratorContext(
                                                instanceId, logicExecutionContext))));
                  }
                } catch (Exception e) {
                  log.error(
                      "Error in getSessionScopedLogicDecorators for decorator : {} and config : {}",
                      s,
                      decoratorConfig,
                      e);
                }
              });
          return ImmutableMap.copyOf(decorators);
        });
  }

  public boolean doDecoratorsNeedFlushing() {
    if (decoratorsNeedFlushing == null) {
      for (List<OutputLogicDecoratorConfig> value : requestScopedLogicDecoratorConfigs.values()) {
        for (OutputLogicDecoratorConfig outputLogicDecoratorConfig : value) {
          if (outputLogicDecoratorConfig.enableFlushing()) {
            decoratorsNeedFlushing = true;
            break;
          }
        }
      }
      if (decoratorsNeedFlushing == null) {
        for (OutputLogicDecoratorConfig value : sessionScopedLogicDecoratorConfigs.values()) {
          if (value.enableFlushing()) {
            decoratorsNeedFlushing = true;
            break;
          }
        }
      }
      if (decoratorsNeedFlushing == null) {
        decoratorsNeedFlushing = false;
      }
    }
    return decoratorsNeedFlushing;
  }

  public void registerRequestScopedDecorator(
      Collection<OutputLogicDecoratorConfig> decoratorConfigs) {
    requestScopedLogicDecoratorConfigs =
        ImmutableMap.<String, List<OutputLogicDecoratorConfig>>builderWithExpectedSize(
                requestScopedLogicDecoratorConfigs.size() + decoratorConfigs.size())
            .putAll(requestScopedLogicDecoratorConfigs)
            .putAll(getDecoratorConfigMap(decoratorConfigs))
            .build();
  }

  private Map<String, List<OutputLogicDecoratorConfig>> getDecoratorConfigMap(
      Collection<OutputLogicDecoratorConfig> decoratorConfigs) {
    Map<String, List<OutputLogicDecoratorConfig>> decoratorConfigMap = new HashMap<>();
    for (OutputLogicDecoratorConfig outputLogicDecoratorConfig : decoratorConfigs) {
      decoratorConfigMap.putIfAbsent(outputLogicDecoratorConfig.decoratorType(), new ArrayList<>());
      decoratorConfigMap
          .get(outputLogicDecoratorConfig.decoratorType())
          .add(outputLogicDecoratorConfig);
    }
    return decoratorConfigMap;
  }

  public void registerSessionScopedLogicDecorator(OutputLogicDecoratorConfig decoratorConfig) {
    sessionScopedLogicDecoratorConfigs.put(decoratorConfig.decoratorType(), decoratorConfig);
  }
}

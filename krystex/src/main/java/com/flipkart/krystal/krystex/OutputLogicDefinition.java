package com.flipkart.krystal.krystex;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.LogicDecoratorContext;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract sealed class OutputLogicDefinition<T> extends LogicDefinition<OutputLogic<T>>
    permits IOLogicDefinition, ComputeLogicDefinition {

  protected OutputLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<Facet> usedFacets,
      ElementTags tags,
      OutputLogic<T> outputLogic) {
    super(kryonLogicId, usedFacets, tags, outputLogic);
  }

  @Getter
  private ImmutableMap<String, List<OutputLogicDecoratorConfig>>
      requestScopedLogicDecoratorConfigs = ImmutableMap.of();

  /** LogicDecorator Id -> LogicDecorator */
  private final Map<String, OutputLogicDecoratorConfig> sessionScopedLogicDecoratorConfigs =
      new HashMap<>();

  private final Map<String, Map<String, OutputLogicDecorator>> sessionScopedDecorators =
      new LinkedHashMap<>();

  public ImmutableMap<String, OutputLogicDecorator> getSessionScopedLogicDecorators(
      KryonDefinition kryonDefinition, DependantChain dependants) {
    Map<String, OutputLogicDecorator> decorators = new LinkedHashMap<>();
    sessionScopedLogicDecoratorConfigs.forEach(
        (s, decoratorConfig) -> {
          try {
            LogicExecutionContext logicExecutionContext =
                new LogicExecutionContext(
                    kryonDefinition.kryonId(),
                    tags(),
                    dependants,
                    kryonDefinition.kryonDefinitionRegistry());
            String instanceId = decoratorConfig.instanceIdGenerator().apply(logicExecutionContext);

            if (decoratorConfig.shouldDecorate().test(logicExecutionContext)) {
              decorators.put(
                  s,
                  sessionScopedDecorators
                      .computeIfAbsent(s, k -> new LinkedHashMap<>())
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

package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig.DecoratorContext;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.model.DependantChain;
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
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract sealed class MainLogicDefinition<T> extends LogicDefinition<MainLogic<T>>
    permits IOLogicDefinition, ComputeLogicDefinition {

  protected MainLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<String> inputs,
      ImmutableMap<Object, Tag> logicTags,
      MainLogic<T> mainLogic) {
    super(kryonLogicId, inputs, logicTags, mainLogic);
  }

  public final ImmutableMap<Inputs, CompletableFuture<@Nullable T>> execute(
      ImmutableList<Inputs> inputs) {
    return logic().execute(inputs);
  }

  @Getter
  private ImmutableMap<String, List<MainLogicDecoratorConfig>> requestScopedLogicDecoratorConfigs =
      ImmutableMap.of();

  /** LogicDecorator Id -> LogicDecorator */
  private final Map<String, MainLogicDecoratorConfig> sessionScopedLogicDecoratorConfigs =
      new HashMap<>();

  private final Map<String, Map<String, MainLogicDecorator>> sessionScopedDecorators =
      new LinkedHashMap<>();

  public ImmutableMap<String, MainLogicDecorator> getSessionScopedLogicDecorators(
      KryonDefinition kryonDefinition, DependantChain dependants) {
    Map<String, MainLogicDecorator> decorators = new LinkedHashMap<>();
    sessionScopedLogicDecoratorConfigs.forEach(
        (s, decoratorConfig) -> {
          LogicExecutionContext logicExecutionContext =
              new LogicExecutionContext(
                  kryonDefinition.kryonId(),
                  logicTags(),
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
                                .apply(new DecoratorContext(instanceId, logicExecutionContext))));
          }
        });
    return ImmutableMap.copyOf(decorators);
  }

  public void registerRequestScopedDecorator(
      Collection<MainLogicDecoratorConfig> decoratorConfigs) {
    //noinspection UnstableApiUsage
    requestScopedLogicDecoratorConfigs =
        ImmutableMap.<String, List<MainLogicDecoratorConfig>>builderWithExpectedSize(
                requestScopedLogicDecoratorConfigs.size() + decoratorConfigs.size())
            .putAll(requestScopedLogicDecoratorConfigs)
            .putAll(getDecoratorConfigMap(decoratorConfigs))
            .build();
  }

  private Map<String, List<MainLogicDecoratorConfig>> getDecoratorConfigMap(
      Collection<MainLogicDecoratorConfig> decoratorConfigs) {
    Map<String, List<MainLogicDecoratorConfig>> decoratorConfigMap = new HashMap<>();
    for (MainLogicDecoratorConfig mainLogicDecoratorConfig : decoratorConfigs) {
      decoratorConfigMap.putIfAbsent(mainLogicDecoratorConfig.decoratorType(), new ArrayList<>());
      decoratorConfigMap
          .get(mainLogicDecoratorConfig.decoratorType())
          .add(mainLogicDecoratorConfig);
    }
    return decoratorConfigMap;
  }

  public void registerSessionScopedLogicDecorator(MainLogicDecoratorConfig decoratorConfig) {
    sessionScopedLogicDecoratorConfigs.put(decoratorConfig.decoratorType(), decoratorConfig);
  }
}

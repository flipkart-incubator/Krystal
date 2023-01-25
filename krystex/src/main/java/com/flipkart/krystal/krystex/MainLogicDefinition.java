package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.node.NodeDefinition;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.logic.LogicTag;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract sealed class MainLogicDefinition<T> extends LogicDefinition
    permits IOLogicDefinition, ComputeLogicDefinition {

  public MainLogicDefinition(
      NodeLogicId nodeLogicId, Set<String> inputs, ImmutableMap<String, LogicTag> logicTags) {
    super(nodeLogicId, inputs, logicTags);
  }

  public abstract ImmutableMap<Inputs, CompletableFuture<T>> execute(ImmutableList<Inputs> inputs);

  private final Map<String, MainLogicDecoratorConfig>
      requestScopedLogicDecoratorConfigs = new HashMap<>();

  /** LogicDecorator Id -> LogicDecorator */
  private final Map<String, MainLogicDecoratorConfig>
      sessionScopedLogicDecoratorConfigs = new HashMap<>();

  private final Map<String, Map<String, MainLogicDecorator>> sessionScopedDecorators =
      new LinkedHashMap<>();

  /** Tag Key -> { LogicDecorator -> Node Decorator Supplier }. */
  public ImmutableMap<String, MainLogicDecoratorConfig>
      getRequestScopedLogicDecoratorConfigs() {
    return ImmutableMap.copyOf(requestScopedLogicDecoratorConfigs);
  }

  public ImmutableMap<String, MainLogicDecorator> getSessionScopedLogicDecorators(
      NodeDefinition nodeDefinition, List<NodeId> dependants) {
    Map<String, MainLogicDecorator> decorators = new LinkedHashMap<>();
    sessionScopedLogicDecoratorConfigs.forEach(
        (s, decoratorConfig) -> {
          LogicExecutionContext logicExecutionContext =
              new LogicExecutionContext(
                  nodeDefinition.nodeId(),
                  logicTags(),
                  dependants,
                  nodeDefinition.nodeDefinitionRegistry());
          if (decoratorConfig.shouldDecorate().test(logicExecutionContext)) {
            String instanceId = decoratorConfig.instanceIdGenerator().apply(logicExecutionContext);
            decorators.put(
                s,
                sessionScopedDecorators
                    .computeIfAbsent(s, k -> new LinkedHashMap<>())
                    .computeIfAbsent(instanceId, k -> decoratorConfig.factory().apply(k)));
          }
        });
    return ImmutableMap.copyOf(decorators);
  }

  public void registerRequestScopedDecorator(
      MainLogicDecoratorConfig decoratorConfig) {
    requestScopedLogicDecoratorConfigs.put(decoratorConfig.decoratorType(), decoratorConfig);
  }

  public void registerSessionScopedLogicDecorator(
      MainLogicDecoratorConfig decoratorConfig) {
    sessionScopedLogicDecoratorConfigs.put(decoratorConfig.decoratorType(), decoratorConfig);
  }
}

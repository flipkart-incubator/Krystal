package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.decoration.RequestScopedMainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.logic.LogicTag;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
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

  private final Map<String, RequestScopedMainLogicDecoratorConfig>
      requestScopedLogicDecoratorConfigs = new HashMap<>();

  /** LogicDecorator Id -> LogicDecorator */
  private final Map<String, MainLogicDecorator> sessionScopedLogicDecorators = new HashMap<>();

  /** Tag Key -> { LogicDecorator -> Node Decorator Supplier }. */
  public ImmutableMap<String, RequestScopedMainLogicDecoratorConfig>
      getRequestScopedLogicDecoratorConfigs() {
    return ImmutableMap.copyOf(requestScopedLogicDecoratorConfigs);
  }

  public Map<String, MainLogicDecorator> getSessionScopedLogicDecorators() {
    return ImmutableMap.copyOf(sessionScopedLogicDecorators);
  }

  public void registerRequestScopedDecorator(
      RequestScopedMainLogicDecoratorConfig decoratorConfig) {
    requestScopedLogicDecoratorConfigs.put(decoratorConfig.decoratorType(), decoratorConfig);
  }

  public void registerSessionScopedLogicDecorator(MainLogicDecorator logicDecorator) {
    sessionScopedLogicDecorators.put(logicDecorator.decoratorType(), logicDecorator);
  }
}

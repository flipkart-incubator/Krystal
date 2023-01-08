package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract sealed class MainLogicDefinition<T> extends LogicDefinition
    permits IOLogicDefinition, ComputeLogicDefinition {

  public MainLogicDefinition(NodeLogicId nodeLogicId, Set<String> inputs) {
    super(nodeLogicId, inputs);
  }

  abstract ImmutableMap<Inputs, CompletableFuture<T>> execute(ImmutableList<Inputs> inputs);

  private final Map<String, Supplier<MainLogicDecorator<T>>> requestScopedNodeDecoratorFactories =
      new HashMap<>();

  /** Group type -> { NodeDecoratorId -> Node Decorator Supplier }. */
  public ImmutableMap<String, Supplier<MainLogicDecorator<T>>> getRequestScopedNodeDecoratorFactories() {
    return ImmutableMap.copyOf(requestScopedNodeDecoratorFactories);
  }

  public void registerRequestScopedNodeDecorator(Supplier<MainLogicDecorator<T>> decoratorFactory) {
    requestScopedNodeDecoratorFactories.put(decoratorFactory.get().getId(), decoratorFactory);
  }
}

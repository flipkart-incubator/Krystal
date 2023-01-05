package com.flipkart.krystal.krystex.node;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public abstract class NodeLogicDefinition<T> {

  private final NodeLogicId nodeLogicId;
  private final Set<String> inputNames = new LinkedHashSet<>();

  private final Map<String, Supplier<NodeDecorator<T>>> requestScopedNodeDecoratorFactories =
      new HashMap<>();

  protected NodeLogicDefinition(NodeLogicId nodeLogicId, Set<String> inputNames) {
    this.nodeLogicId = nodeLogicId;
    this.inputNames.addAll(inputNames);
  }

  public abstract NodeLogic<T> logic();

  public NodeLogicId nodeLogicId() {
    return nodeLogicId;
  }

  public ImmutableSet<String> inputNames() {
    return ImmutableSet.copyOf(inputNames);
  }

  /** Group type -> { NodeDecoratorId -> Node Decorator Supplier }. */
  public ImmutableMap<String, Supplier<NodeDecorator<T>>> getRequestScopedNodeDecoratorFactories() {
    return ImmutableMap.copyOf(requestScopedNodeDecoratorFactories);
  }

  public void registerRequestScopedNodeDecorator(Supplier<NodeDecorator<T>> decoratorFactory) {
    requestScopedNodeDecoratorFactories.put(decoratorFactory.get().getId(), decoratorFactory);
  }
}

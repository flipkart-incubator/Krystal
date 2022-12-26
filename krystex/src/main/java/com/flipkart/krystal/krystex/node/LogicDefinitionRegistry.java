package com.flipkart.krystal.krystex.node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class LogicDefinitionRegistry {
  private final Map<NodeLogicId, NodeLogicDefinition<?>> nodeDefinitions = new HashMap<>();

  public LogicDefinitionRegistry() {}

  public <T> NodeLogicDefinition<T> get(NodeLogicId nodeLogicId) {
    //noinspection unchecked
    return (NodeLogicDefinition<T>) nodeDefinitions.get(nodeLogicId);
  }

  public <T> ComputeLogicDefinition<T> newComputeLogic(
      String nodeId, Function<NodeInputs, T> logic) {
    return newComputeLogic(nodeId, Set.of(), logic);
  }

  public <T> ComputeLogicDefinition<T> newComputeLogic(
      String nodeId, Set<String> inputs, Function<NodeInputs, T> logic) {
    return newBatchComputeLogic(nodeId, inputs, logic.andThen(ImmutableList::of));
  }

  public <T> ComputeLogicDefinition<T> newBatchComputeLogic(
      String nodeId, Set<String> inputs, Function<NodeInputs, ImmutableList<T>> logic) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(new NodeLogicId(nodeId), inputs, logic);
    add(def);
    return def;
  }

  public void add(NodeLogicDefinition<?> nodeLogicDefinition) {
    if (nodeDefinitions.containsKey(nodeLogicDefinition.nodeId())) {
      return;
    }
    nodeDefinitions.put(nodeLogicDefinition.nodeId(), nodeLogicDefinition);
  }

  public void validate() {
    // TODO Check if all dependencies are present - there should be no dangling node ids
    // TODO Check that there are no loops in dependencies.
  }

  public <T> IOLogicDefinition<T> newIOLogic(
      NodeLogicId nodeLogicId, Set<String> inputs, NodeLogic<T> nodeLogic) {
    IOLogicDefinition<T> def =
        new IOLogicDefinition<T>(nodeLogicId, inputs, ImmutableMap.of(), nodeLogic);
    add(def);
    return def;
  }
}

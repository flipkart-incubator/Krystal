package com.flipkart.krystal.krystex.node;

import java.util.HashMap;
import java.util.Map;

public final class LogicDefinitionRegistry {
  private final Map<NodeLogicId, NodeLogicDefinition<?>> nodeDefinitions = new HashMap<>();

  public LogicDefinitionRegistry() {}

  public <T> NodeLogicDefinition<T> get(NodeLogicId nodeLogicId) {
    //noinspection unchecked
    return (NodeLogicDefinition<T>) nodeDefinitions.get(nodeLogicId);
  }

  public void add(NodeLogicDefinition<?> nodeLogicDefinition) {
    if (nodeDefinitions.containsKey(nodeLogicDefinition.nodeLogicId())) {
      return;
    }
    nodeDefinitions.put(nodeLogicDefinition.nodeLogicId(), nodeLogicDefinition);
  }

  public void validate() {
    // TODO Check if all dependencies are present - there should be no dangling node ids
    // TODO Check that there are no loops in dependencies.
  }
}

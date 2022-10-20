package com.flipkart.krystal.krystex;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class NodeRegistry {
  private final NodeDefinitions nodeDefinitions;
  private final Map<String, Node<?>> nodes = new HashMap<>();

  public NodeRegistry(NodeDefinitions nodeDefinitions) {
    this.nodeDefinitions = nodeDefinitions;
  }

  public <T> Node<T> get(String nodeId) {
    //noinspection unchecked
    return (Node<T>) nodes.get(nodeId);
  }

  public <T> Set<Node<?>> getAll(Set<String> nodeIds) {
    return nodeIds.stream().map(this::get).collect(Collectors.toSet());
  }
}

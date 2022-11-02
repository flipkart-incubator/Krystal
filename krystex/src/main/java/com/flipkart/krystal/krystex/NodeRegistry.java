package com.flipkart.krystal.krystex;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.Getter;

public final class NodeRegistry {
  @Getter private final NodeDefinitionRegistry nodeDefinitionRegistry;
  private final Map<String, Node<?>> nodes = new HashMap<>();

  public NodeRegistry(NodeDefinitionRegistry nodeDefinitionRegistry) {
    this.nodeDefinitionRegistry = nodeDefinitionRegistry;
  }

  public void add(Node<?> node) {
    nodes.put(node.getNodeId(), node);
  }

  public <T> Node<T> get(String nodeId) {
    //noinspection unchecked
    return (Node<T>) nodes.get(nodeId);
  }

  public <T> ImmutableMap<String, Node<?>> getAll(Set<String> nodeIds) {
    return nodeIds.stream().collect(toImmutableMap(Function.identity(), this::get));
  }
}

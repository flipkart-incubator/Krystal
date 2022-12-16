package com.flipkart.krystal.krystex.node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class NodeRegistry {

  private final Map<NodeId, Node> nodes = new LinkedHashMap<>();

  public NodeRegistry() {}

  public Node get(NodeId nodeId) {
    Node node = nodes.get(nodeId);
    if (node == null) {
      throw new IllegalArgumentException("No Node with id %s found".formatted(nodeId));
    }
    return node;
  }

  public Node createIfAbsent(NodeId nodeId, Function<NodeId, Node> supplier) {
    return nodes.computeIfAbsent(nodeId, supplier);
  }
}

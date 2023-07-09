package com.flipkart.krystal.krystex.node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class NodeRegistry {

  private final Map<NodeId, GranularNode> nodes = new LinkedHashMap<>();

  public NodeRegistry() {}

  public GranularNode get(NodeId nodeId) {
    return tryGet(nodeId)
        .orElseThrow(
            () -> new IllegalArgumentException("No Node with id %s found".formatted(nodeId)));
  }

  public Optional<GranularNode> tryGet(NodeId nodeId) {
    return Optional.ofNullable(nodes.get(nodeId));
  }

  public GranularNode createIfAbsent(NodeId nodeId, Function<NodeId, GranularNode> supplier) {
    return nodes.computeIfAbsent(nodeId, supplier);
  }
}

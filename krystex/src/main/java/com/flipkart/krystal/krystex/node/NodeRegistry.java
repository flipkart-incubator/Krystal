package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.commands.NodeCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

final class NodeRegistry<T extends Node<? extends NodeCommand,? extends NodeResponse>> {

  private final Map<NodeId, T> nodes = new LinkedHashMap<>();

  public T get(NodeId nodeId) {
    return tryGet(nodeId)
        .orElseThrow(
            () -> new IllegalArgumentException("No Node with id %s found".formatted(nodeId)));
  }

  public Optional<T> tryGet(NodeId nodeId) {
    return Optional.ofNullable(nodes.get(nodeId));
  }

  public T createIfAbsent(NodeId nodeId, Function<NodeId, ? extends T> supplier) {
    return nodes.computeIfAbsent(nodeId, supplier);
  }
}

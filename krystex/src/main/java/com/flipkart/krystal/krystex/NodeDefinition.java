package com.flipkart.krystal.krystex;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract sealed class NodeDefinition<T>
    permits BlockingNodeDefinition, NonBlockingNodeDefinition {

  private final String nodeId;
  private final Set<String> inputs;
  private final Set<String> dependants = new LinkedHashSet<>();

  NodeDefinition(String nodeId, Set<String> inputs) {
    this.nodeId = nodeId;
    this.inputs = inputs;
  }

  public void isAnInputTo(String nodeId) {
    dependants.add(nodeId);
  }

  public abstract CompletableFuture<T> logic();

  public String nodeId() {
    return nodeId;
  }

  public Set<String> inputs() {
    return inputs;
  }

  public Set<String> dependants() {
    return dependants;
  }
}

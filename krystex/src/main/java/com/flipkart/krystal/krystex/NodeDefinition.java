package com.flipkart.krystal.krystex;

import io.github.resilience4j.ratelimiter.RateLimiter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

  protected abstract CompletableFuture<T> logic();

  public String nodeId() {
    return nodeId;
  }

  public Set<String> inputs() {
    return inputs;
  }

  public Set<String> dependants() {
    return dependants;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (NodeDefinition) obj;
    return Objects.equals(this.nodeId, that.nodeId)
        && Objects.equals(this.inputs, that.inputs)
        && Objects.equals(this.dependants, that.dependants);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeId, inputs, dependants);
  }

  @Override
  public String toString() {
    return "NodeDefinition["
        + "nodeId="
        + nodeId
        + ", "
        + "inputs="
        + inputs
        + ", "
        + "dependants="
        + dependants
        + ']';
  }
}

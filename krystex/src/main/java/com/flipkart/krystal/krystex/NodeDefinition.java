package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract sealed class NodeDefinition<T>
    permits BlockingNodeDefinition, NonBlockingNodeDefinition {

  private final String nodeId;
  private final Map<String, String> inputs;
  private final Set<String> dependants = new LinkedHashSet<>();

  NodeDefinition(String nodeId, Map<String, String> inputs) {
    this.nodeId = nodeId;
    this.inputs = new LinkedHashMap<>(inputs);
  }

  void isAnInputTo(String nodeId) {
    dependants.add(nodeId);
  }

  public void addInputProvider(String inputName, @NonNull String nodeId) {
    if (inputs.containsKey(inputName)) {
      throw new IllegalArgumentException("Input %s already has a provider node registered");
    }
    inputs.put(inputName, nodeId);
  }

  public abstract CompletableFuture<ImmutableList<T>> logic(
      ImmutableMap<String, ?> dependencyValues);

  public String nodeId() {
    return nodeId;
  }

  public ImmutableMap<String, String> inputs() {
    return ImmutableMap.copyOf(inputs);
  }

  public Set<String> dependants() {
    return dependants;
  }
}

package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract sealed class NodeDefinition<T>
    permits BlockingNodeDefinition, NonBlockingNodeDefinition {

  private final String nodeId;
  private final Set<String> inputNames = new LinkedHashSet<>();
  private final Map<String, String> inputNamesToProvider = new LinkedHashMap<>();
  private final Set<String> dependants = new LinkedHashSet<>();

  NodeDefinition(String nodeId, Set<String> inputNames, Map<String, String> inputNamesToProvider) {
    this.nodeId = nodeId;
    this.inputNamesToProvider.putAll(inputNamesToProvider);
    this.inputNames.addAll(inputNames);
  }

  void isAnInputTo(String nodeId) {
    dependants.add(nodeId);
  }

  public void addInputWithoutProvider(String inputName) {
    if (inputNames.contains(inputName)) {
      throw new IllegalArgumentException("Input %s has already been added");
    }
    inputNames.add(inputName);
  }

  public void addInputProvider(String inputName, @NonNull String nodeId) {
    if (inputNamesToProvider.containsKey(inputName)) {
      throw new IllegalArgumentException("Input %s already has a provider node registered");
    }
    addInputWithoutProvider(inputName);
    inputNamesToProvider.put(inputName, nodeId);
  }

  public abstract CompletableFuture<ImmutableList<T>> logic(
      ImmutableMap<String, ?> dependencyValues);

  public String nodeId() {
    return nodeId;
  }

  public ImmutableMap<String, String> inputProviders() {
    return ImmutableMap.copyOf(inputNamesToProvider);
  }

  public ImmutableSet<String> inputNames() {
    return ImmutableSet.copyOf(inputNames);
  }
}

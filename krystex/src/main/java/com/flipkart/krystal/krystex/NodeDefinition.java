package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract sealed class NodeDefinition<T>
    permits BlockingNodeDefinition, NonBlockingNodeDefinition {

  private final String nodeId;
  private final Set<String> dependencyNames = new LinkedHashSet<>();
  private final Set<String> inputNames = new LinkedHashSet<>();

  private final Map<String, String> dependencyNamesToProvider = new LinkedHashMap<>();
  private final Set<String> dependants = new LinkedHashSet<>();
  /* Used in input binders where a node can adopt input of another node based on binding */
  private final Map<String, Map<String, NodeDefinition<?>>> inputAdaptionTarget = new LinkedHashMap<>();

  NodeDefinition(String nodeId, Set<String> dependencyNames, Map<String, String> dependencyNamesToProvider, Set<String> inputNames) {
    this.nodeId = nodeId;
    this.dependencyNamesToProvider.putAll(dependencyNamesToProvider);
    this.dependencyNames.addAll(dependencyNames);
    this.inputNames.addAll(inputNames);
  }

  void isADependencyTo(String nodeId) {
    dependants.add(nodeId);
  }

  public void addDependencyWithoutProvider(String dependencyName) {
    if (dependencyNames.contains(dependencyName)) {
      throw new IllegalArgumentException("Dependency %s has already been added");
    }
    dependencyNames.add(dependencyName);
  }

  public void addDependencyProvider(String dependencyName, @NonNull String nodeId) {
    if (dependencyNamesToProvider.containsKey(dependencyName)) {
      throw new IllegalArgumentException("Dependency %s already has a provider node registered");
    }
    if (!dependencyNames.contains(dependencyName)) {
      addDependencyWithoutProvider(dependencyName);
    }
    dependencyNamesToProvider.put(dependencyName, nodeId);
  }


  public abstract CompletableFuture<ImmutableList<T>> logic(
      ImmutableMap<String, ?> dependencyValues);

  public String nodeId() {
    return nodeId;
  }

  public ImmutableMap<String, String> dependencyProviders() {
    return ImmutableMap.copyOf(dependencyNamesToProvider);
  }

  public ImmutableSet<String> dependencyNames() {
    return ImmutableSet.copyOf(dependencyNames);
  }

  public ImmutableSet<String> inputNames() {
    return ImmutableSet.copyOf(inputNames);
  }

  public void addInputAdaptionTarget(String depName, String targetInputName, NodeDefinition<?> targetNodeId) {

    if (!inputNames.contains(targetInputName)) {
      inputNames.add(depName);
    }
    inputAdaptionTarget.computeIfAbsent(depName, value -> new HashMap<>());
    inputAdaptionTarget.get(depName).put(targetInputName, targetNodeId);
  }

  public ImmutableMap<String, NodeDefinition<?>> inputAdaptionTarget(String inputName) {
    return ImmutableMap.copyOf(inputAdaptionTarget.getOrDefault(inputName, ImmutableMap.of()));
  }
}

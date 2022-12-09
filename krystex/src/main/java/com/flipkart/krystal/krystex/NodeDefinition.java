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
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract sealed class NodeDefinition<T> permits IONodeDefinition, NonBlockingNodeDefinition {

  private final String nodeId;
  private final Set<String> inputNames = new LinkedHashSet<>();
  private final Map<String, String> inputNamesToProvider = new LinkedHashMap<>();
  private final Set<String> dependants = new LinkedHashSet<>();

  /**
   * Group type -> { NodeDecoratorId -> Node Decorator Supplier }.
   *
   * <p>This is used in the request context to create NodeDecorators
   */
  private final Map<String, ImmutableMap<String, Supplier<NodeDecorator<T>>>>
      requestScopedDecoratorSuppliers = new HashMap<>();

  private final ImmutableMap<String, String> groupMemberships;

  NodeDefinition(
      String nodeId,
      Set<String> inputNames,
      Map<String, String> inputNamesToProvider,
      ImmutableMap<String, String> groupMemberships) {
    this.nodeId = nodeId;
    this.groupMemberships = groupMemberships;
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

  /** Group type -> { NodeDecoratorId -> Node Decorator Supplier }. */
  public ImmutableMap<String, ImmutableMap<String, Supplier<NodeDecorator<T>>>>
      getRequestScopedNodeDecoratorSuppliers() {
    return ImmutableMap.copyOf(requestScopedDecoratorSuppliers);
  }

  public ImmutableMap<String, String> getGroupMemberships() {
    return groupMemberships;
  }
}

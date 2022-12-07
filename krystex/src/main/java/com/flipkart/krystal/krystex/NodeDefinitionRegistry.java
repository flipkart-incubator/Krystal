package com.flipkart.krystal.krystex;

import static java.util.Collections.emptySet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class NodeDefinitionRegistry {
  private final Map<String, NodeDefinition<?>> nodeDefinitions = new HashMap<>();
  private final Map<String, Set<NodeDefinition<?>>> nodeDefinitionsByDependencyNodes = new HashMap<>();
  @Nullable
  private NodeDefinitionRegistry backingRegistry;

  public NodeDefinitionRegistry(@NonNull NodeDefinitionRegistry backingRegistry) {
    this.backingRegistry = backingRegistry;
  }

  public NodeDefinitionRegistry() {
  }

  public NodeDefinition<?> get(String nodeId) {
    return nodeDefinitions.get(nodeId);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingNode(
      String nodeId, Function<ImmutableMap<String, ?>, T> logic) {
    return newNonBlockingNode(nodeId, Set.of(), logic);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingNode(
      String nodeId, Set<String> inputs, Function<ImmutableMap<String, ?>, T> logic) {
    return newNonBlockingNode(nodeId, ImmutableSet.of(), ImmutableMap.of(), inputs, logic);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingNode(
      String nodeId,
      Set<String> dependencies,
      Map<String, String> dependencyProviders,
      Set<String> inputs,
      Function<ImmutableMap<String, ?>, T> logic) {
    return newNonBlockingBatchNode(
        nodeId, dependencies, dependencyProviders, inputs,
        logic.andThen(ImmutableList::of));
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingBatchNode(
      String nodeId, Function<ImmutableMap<String, ?>, ImmutableList<T>> logic) {
    return newNonBlockingBatchNode(nodeId, ImmutableSet.of(), ImmutableMap.of(), ImmutableSet.of(),
        logic);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingBatchNode(
      String nodeId,
      Set<String> dependencies,
      Map<String, String> dependencyProviders,
      Set<String> inputs,
      Function<ImmutableMap<String, ?>, ImmutableList<T>> logic) {
    Set<String> dependenciesAndInputs = new HashSet<>();
    dependenciesAndInputs.addAll(dependencies);
    dependenciesAndInputs.addAll(inputs);

    NonBlockingNodeDefinition<T> def =
        new NonBlockingNodeDefinition<>(nodeId, dependencies, dependencyProviders, inputs) {
          @Override
          protected ImmutableList<T> nonBlockingLogic(ImmutableMap<String, ?> dependencyValues) {
            return logic.apply(dependencyValues);
          }
        };
    add(def);
    return def;
  }

  public <T> BlockingNodeDefinition<T> newBlockingBatchNode(
      String nodeId,
      Set<String> dependencies,
      Map<String, String> dependencyProviders,
      Set<String> inputs,
      Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> logic) {
    Set<String> dependenciesAndInputs = new HashSet<>();
    dependenciesAndInputs.addAll(dependencies);
    dependenciesAndInputs.addAll(inputs);

    BlockingNodeDefinition<T> def =
        new BlockingNodeDefinition<>(nodeId, dependencies, dependencyProviders, inputs) {
          @Override
          protected CompletableFuture<ImmutableList<T>> blockingLogic(ImmutableMap<String, ?> dependencyValues) {
            return logic.apply(dependencyValues);
          }
        };
    add(def);
    return def;
  }

  public void add(NodeDefinition<?> nodeDefinition) {
    nodeDefinitions.put(nodeDefinition.nodeId(), nodeDefinition);
    nodeDefinition
        .dependencyProviders()
        .values()
        .forEach(
            inputNode -> {
              nodeDefinitionsByDependencyNodes
                  .computeIfAbsent(inputNode, s -> new HashSet<>())
                  .add(nodeDefinition);
              NodeDefinition<?> inputNodeDef = nodeDefinitions.get(inputNode);
              if (inputNodeDef != null) {
                inputNodeDef.isADependencyTo(nodeDefinition.nodeId());
              }
            });
    nodeDefinitionsByDependencyNodes
        .getOrDefault(nodeDefinition.nodeId(), emptySet())
        .forEach(dependent -> nodeDefinition.isADependencyTo(dependent.nodeId()));
    getBackingRegistry().ifPresent(backingRegistry -> backingRegistry.add(nodeDefinition));
  }

  private Optional<NodeDefinitionRegistry> getBackingRegistry() {
    return Optional.ofNullable(backingRegistry);
  }

  public void validate() {
    // TODO Check if all dependencies are present - there should be no dangling node ids
    // TODO Check that there are no loops in dependencies.
  }
}

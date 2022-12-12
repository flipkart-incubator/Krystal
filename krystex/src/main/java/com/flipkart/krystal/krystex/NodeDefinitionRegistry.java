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
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class NodeDefinitionRegistry {
  private final Map<String, NodeDefinition<?>> nodeDefinitions = new HashMap<>();
  private final Map<String, Set<NodeDefinition<?>>> nodeDefinitionsByInputNodes = new HashMap<>();
  @Nullable private NodeDefinitionRegistry backingRegistry;

  public NodeDefinitionRegistry(@NonNull NodeDefinitionRegistry backingRegistry) {
    this.backingRegistry = backingRegistry;
  }

  public NodeDefinitionRegistry() {}

  public NodeDefinition<?> get(String nodeId) {
    return nodeDefinitions.get(nodeId);
  }

  /**
   * Creates a non-blocking nodeDefinition which has no logic. Any attempt to execute the logic of
   * the returned node definition will throw an UnsupportedOperationException. The only way a node
   * of this definition can be terminated is my setting its value explicitly from outside the node
   * execution graph.
   */
  public <T> NonBlockingNodeDefinition<T> newNonBlockingNode(String nodeId) {
    return newNonBlockingNode(
        nodeId,
        nodeInputs -> {
          throw new UnsupportedOperationException();
        });
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingNode(
      String nodeId, Function<NodeInputs, T> logic) {
    return newNonBlockingNode(nodeId, Set.of(), logic);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingNode(
      String nodeId, Set<String> inputs, Function<NodeInputs, T> logic) {
    return newNonBlockingNode(nodeId, inputs, ImmutableMap.of(), logic);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingNode(
      String nodeId, Map<String, String> inputProviders, Function<NodeInputs, T> logic) {
    return newNonBlockingNode(nodeId, inputProviders.keySet(), inputProviders, logic);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingNode(
      String nodeId,
      Set<String> inputs,
      Map<String, String> inputProviders,
      Function<NodeInputs, T> logic) {
    return newNonBlockingBatchNode(
        nodeId, inputs, inputProviders, logic.andThen(ImmutableList::of));
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingBatchNode(
      String nodeId, Function<NodeInputs, ImmutableList<T>> logic) {
    return newNonBlockingBatchNode(nodeId, ImmutableSet.of(), ImmutableMap.of(), logic);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingBatchNode(
      String nodeId,
      Set<String> inputs,
      Map<String, String> inputProviders,
      Function<NodeInputs, ImmutableList<T>> logic) {
    NonBlockingNodeDefinition<T> def =
        new NonBlockingNodeDefinition<>(nodeId, inputs, inputProviders, logic);
    add(def);
    return def;
  }

  public void add(NodeDefinition<?> nodeDefinition) {
    if (nodeDefinitions.containsKey(nodeDefinition.nodeId())) {
      return;
    }
    nodeDefinitions.put(nodeDefinition.nodeId(), nodeDefinition);
    nodeDefinition
        .inputProviders()
        .values()
        .forEach(
            inputNode -> {
              nodeDefinitionsByInputNodes
                  .computeIfAbsent(inputNode, s -> new HashSet<>())
                  .add(nodeDefinition);
              NodeDefinition<?> inputNodeDef = nodeDefinitions.get(inputNode);
              if (inputNodeDef != null) {
                inputNodeDef.isAnInputTo(nodeDefinition.nodeId());
              }
            });
    nodeDefinitionsByInputNodes
        .getOrDefault(nodeDefinition.nodeId(), emptySet())
        .forEach(dependent -> nodeDefinition.isAnInputTo(dependent.nodeId()));
    getBackingRegistry().ifPresent(backingRegistry -> backingRegistry.add(nodeDefinition));
  }

  private Optional<NodeDefinitionRegistry> getBackingRegistry() {
    return Optional.ofNullable(backingRegistry);
  }

  public void validate() {
    // TODO Check if all dependencies are present - there should be no dangling node ids
    // TODO Check that there are no loops in dependencies.
  }

  public <T> IONodeDefinition<T> newIONodeDefinition(
      String nodeId,
      Set<String> inputs,
      Map<String, String> inputProviders,
      NodeLogic<T> nodeLogic) {
    IONodeDefinition<T> def =
        new IONodeDefinition<T>(nodeId, inputs, inputProviders, ImmutableMap.of(), nodeLogic);

    add(def);
    return def;
  }
}

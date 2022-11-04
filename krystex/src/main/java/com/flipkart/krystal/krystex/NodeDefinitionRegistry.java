package com.flipkart.krystal.krystex;

import static java.util.Collections.emptySet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class NodeDefinitionRegistry {
  private final Map<String, NodeDefinition<?>> nodeDefinitions = new HashMap<>();
  private final Map<String, Set<NodeDefinition<?>>> nodeDefinitionsByInputNodes = new HashMap<>();

  public NodeDefinition<?> get(String nodeId) {
    return nodeDefinitions.get(nodeId);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingNode(
      String nodeId, Map<String, String> inputs, Function<ImmutableMap<String, ?>, T> logic) {
    return newNonBlockingBatchNode(nodeId, inputs, logic.andThen(ImmutableList::of));
  }

  public <T> NonBlockingNodeDefinition<T> newUnboundNonBlockingNode(
      String nodeId, Function<ImmutableMap<String, ?>, T> logic) {
    return newNonBlockingBatchNode(nodeId, ImmutableMap.of(), logic.andThen(ImmutableList::of));
  }

  public <T> NonBlockingNodeDefinition<T> newUnboundNonBlockingBatchNode(
      String nodeId, Function<ImmutableMap<String, ?>, ImmutableList<T>> logic) {
    return newNonBlockingBatchNode(nodeId, ImmutableMap.of(), logic);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingBatchNode(
      String nodeId,
      Map<String, String> inputs,
      Function<ImmutableMap<String, ?>, ImmutableList<T>> logic) {
    NonBlockingNodeDefinition<T> def =
        new NonBlockingNodeDefinition<>(nodeId, inputs) {
          @Override
          protected ImmutableList<T> nonBlockingLogic(ImmutableMap<String, ?> dependencyValues) {
            return logic.apply(dependencyValues);
          }
        };
    add(def);
    return def;
  }

  public void add(NodeDefinition<?> nodeDefinition) {
    nodeDefinitions.put(nodeDefinition.nodeId(), nodeDefinition);
    nodeDefinition
        .inputs()
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
  }

  public void validate() {
    // TODO Check if all dependencies are present - there should be no dangling node ids
    // TODO Check that there are no loops in dependencies.
  }
}

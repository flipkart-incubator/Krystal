package com.flipkart.krystal.krystex;

import static java.util.Collections.emptySet;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class NodeDefinitionRegistry {
  private final Map<String, NodeDefinition<?>> nodeDefinitions = new HashMap<>();
  private final Map<String, Set<NodeDefinition<?>>> nodeDefinitionsByInputs = new HashMap<>();

  public NodeDefinition<?> get(String nodeId) {
    return nodeDefinitions.get(nodeId);
  }

  public <T> NonBlockingNodeDefinition<T> newNonBlockingNode(
      String nodeId, Set<String> inputs, Function<ImmutableMap<String, ?>, T> logic) {
    NonBlockingNodeDefinition<T> def =
        new NonBlockingNodeDefinition<>(nodeId, inputs) {
          @Override
          protected T nonBlockingLogic(ImmutableMap<String, ?> dependencyValues) {
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
        .forEach(
            input -> {
              nodeDefinitionsByInputs
                  .computeIfAbsent(input, s -> new HashSet<>())
                  .add(nodeDefinition);
              NodeDefinition<?> inputNodeDef = nodeDefinitions.get(input);
              if (inputNodeDef != null) {
                inputNodeDef.isAnInputTo(nodeDefinition.nodeId());
              }
            });
    nodeDefinitionsByInputs
        .getOrDefault(nodeDefinition.nodeId(), emptySet())
        .forEach(dependent -> nodeDefinition.isAnInputTo(dependent.nodeId()));
  }

  public void validate() {
    // TODO Check if all dependencies are present - there should be no dangling node ids
    // TODO Check that there are no loops in dependencies.
  }
}

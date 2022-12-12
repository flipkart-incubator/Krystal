package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.krystex.NodeDefinitionRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;

public record VajramDAG<T>(
    VajramDefinition vajramDefinition,
    NodeDefinition<T> vajramLogicNodeDefinition,
    ImmutableList<ResolverDefinition> resolverDefinitions,
    ImmutableMap<String, String> dependencies,
    NodeDefinitionRegistry nodeDefinitionRegistry) {

  public VajramDAG<T> addProviderNodes(Map<String, String> inputNameToProviderNode) {
    inputNameToProviderNode.forEach(vajramLogicNodeDefinition()::addInputProvider);
    resolverDefinitions()
        .forEach(
            resolverDefinition -> {
              Set<String> intersection =
                  Sets.intersection(
                      resolverDefinition.boundFrom(), inputNameToProviderNode.keySet());
              if (!intersection.isEmpty()) {
                intersection.forEach(
                    inputName ->
                        resolverDefinition
                            .resolverNode()
                            .addInputProvider(inputName, inputNameToProviderNode.get(inputName)));
              }
            });
    return new VajramDAG<>(
        vajramDefinition,
        vajramLogicNodeDefinition,
        resolverDefinitions,
        ImmutableMap.<String, String>builder()
            .putAll(dependencies)
            .putAll(inputNameToProviderNode)
            .build(),
        nodeDefinitionRegistry);
  }

  record ResolverDefinition(
      NodeDefinition<?> resolverNode,
      ImmutableList<NodeDefinition<?>> extractorNodes,
      ImmutableSet<String> boundFrom) {}
}

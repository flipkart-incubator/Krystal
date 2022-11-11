package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.krystex.NodeDefinitionRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public record VajramDAG<T>(
    VajramDefinition vajramDefinition,
    NodeDefinition<T> vajramLogicNodeDefinition,
    ImmutableList<ResolverDefinition> resolverDefinitions,
    ImmutableMap<String, String> dependencies,
    NodeDefinitionRegistry nodeDefinitionRegistry) {

  public VajramDAG(
      VajramDefinition vajramDefinition, NodeDefinitionRegistry mainNodeDefinitionRegistry) {
    this(
        vajramDefinition,
        null,
        ImmutableList.of(),
        ImmutableMap.of(),
        new NodeDefinitionRegistry(mainNodeDefinitionRegistry));
  }

  record ResolverDefinition(NodeDefinition<?> nodeDefinition, ImmutableSet<String> boundFrom) {}
}

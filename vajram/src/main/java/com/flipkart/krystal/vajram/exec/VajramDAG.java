package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.krystex.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.NonBlockingNodeDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public record VajramDAG<T>(
    VajramDefinition vajramDefinition,
    NodeDefinition<T> vajramLogicNodeDefinition,
    ImmutableList<ResolverDefinition> resolverDefinitions,
    ImmutableList<NonBlockingNodeDefinition<?>> inputAdapterNodes,
    ImmutableMap<String, String> dependencies,
    NodeDefinitionRegistry nodeDefinitionRegistry) {

  public VajramDAG(
      VajramDefinition vajramDefinition, NodeDefinitionRegistry mainNodeDefinitionRegistry) {
    this(
        vajramDefinition,
        null,
        ImmutableList.of(),
        ImmutableList.of(),
        ImmutableMap.of(),
        new NodeDefinitionRegistry(mainNodeDefinitionRegistry));
  }

  record ResolverDefinition(NodeDefinition<?> nodeDefinition, ImmutableSet<String> boundFrom) {}
}

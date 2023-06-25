package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @param dependencyNodes Map of dependency name to nodeId.
 */
public record NodeDefinition(
    NodeId nodeId,
    NodeLogicId mainLogicId,
    ImmutableMap<String, NodeId> dependencyNodes,
    ImmutableList<ResolverDefinition> resolverDefinitions,
    NodeDefinitionRegistry nodeDefinitionRegistry) {

  public <T> MainLogicDefinition<T> getMainLogicDefinition() {
    return nodeDefinitionRegistry().logicDefinitionRegistry().getMain(mainLogicId());
  }
}

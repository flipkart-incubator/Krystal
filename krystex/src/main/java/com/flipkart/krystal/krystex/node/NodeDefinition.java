package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.ResolverDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public record NodeDefinition(
    NodeId nodeId,
    NodeLogicId logicNode,
    ImmutableMap<String, NodeId> dependencyNodes,
    ImmutableList<ResolverDefinition> resolverDefinitions,
    NodeDefinitionRegistry nodeDefinitionRegistry) {}

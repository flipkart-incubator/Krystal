package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class NodeDefinitionRegistry {

  private final LogicDefinitionRegistry logicDefinitionRegistry;
  private final Map<NodeId, NodeDefinition> nodeDefinitions = new LinkedHashMap<>();

  public NodeDefinitionRegistry(LogicDefinitionRegistry logicDefinitionRegistry) {
    this.logicDefinitionRegistry = logicDefinitionRegistry;
  }

  public LogicDefinitionRegistry logicDefinitionRegistry() {
    return logicDefinitionRegistry;
  }

  public NodeDefinition get(NodeId nodeId) {
    NodeDefinition node = nodeDefinitions.get(nodeId);
    if (node == null) {
      throw new IllegalArgumentException("No Node with id %s found".formatted(nodeId));
    }
    return node;
  }

  public NodeDefinition newNodeDefinition(String nodeId, NodeLogicId mainLogicId) {
    return newNodeDefinition(nodeId, mainLogicId, ImmutableMap.of());
  }

  public NodeDefinition newNodeDefinition(
      String nodeId, NodeLogicId mainLogicId, ImmutableMap<String, NodeId> dependencyNodes) {
    return newNodeDefinition(nodeId, mainLogicId, dependencyNodes, ImmutableList.of(), null);
  }

  public NodeDefinition newNodeDefinition(
      String nodeId,
      NodeLogicId mainLogicId,
      ImmutableMap<String, NodeId> dependencyNodes,
      ImmutableList<ResolverDefinition> resolverDefinitions,
      NodeLogicId multiResolverId) {
    if (!resolverDefinitions.isEmpty() && multiResolverId == null) {
      throw new IllegalArgumentException("missing multi resolver logic");
    }
    NodeDefinition nodeDefinition =
        new NodeDefinition(
            new NodeId(nodeId),
            mainLogicId,
            dependencyNodes,
            resolverDefinitions,
            Optional.ofNullable(multiResolverId),
            this);
    nodeDefinitions.put(nodeDefinition.nodeId(), nodeDefinition);
    return nodeDefinition;
  }
}

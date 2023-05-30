package com.flipkart.krystal.krystex.node;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.krystex.CallGraph;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @param isRecursive {@code true} if this node depends on itself directly or indirectly (a
 *     dependency loop). {@code false} otherwise.
 */
public record NodeDefinition(
    NodeId nodeId,
    NodeLogicId mainLogicId,
    ImmutableMap<String, NodeId> dependencyNodes,
    ImmutableList<ResolverDefinition> resolverDefinitions,
    NodeDefinitionRegistry nodeDefinitionRegistry,
    boolean isRecursive) {

  public NodeDefinition(
      NodeId nodeId,
      NodeLogicId mainLogicId,
      ImmutableMap<String, NodeId> dependencyNodes,
      ImmutableList<ResolverDefinition> resolverDefinitions,
      NodeDefinitionRegistry nodeDefinitionRegistry) {
    this(nodeId, mainLogicId, dependencyNodes, resolverDefinitions, nodeDefinitionRegistry, false);
  }

  /**
   * @return a nodeDefinition which is exactly same as this nodeDefinition except that the returned
   *     one is guaranteed to return {@code true} for {@link #isRecursive()}
   */
  NodeDefinition toRecursive() {
    if (isRecursive()) {
      return this;
    }
    return new NodeDefinition(
        nodeId(),
        mainLogicId(),
        dependencyNodes(),
        resolverDefinitions(),
        nodeDefinitionRegistry(),
        true);
  }

  public CallGraph getCallGraph(CallGraph previousCalls) {
    return new CallGraph(
        nodeId,
        resolverDefinitions.stream()
            .map(
                resolverDefinition -> {
                  String dependencyName = resolverDefinition.dependencyName();
                  NodeId depNodeId = dependencyNodes.get(dependencyName);
                  NodeDefinition depNode = nodeDefinitionRegistry.get(depNodeId);
                  return depNode.getCallGraph(previousCalls);
                })
            .collect(toImmutableList()));
  }

  public <T> MainLogicDefinition<T> getMainLogicDefinition() {
    return nodeDefinitionRegistry().logicDefinitionRegistry().getMain(mainLogicId());
  }
}

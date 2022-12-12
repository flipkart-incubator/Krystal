package com.flipkart.krystal.krystex.nodecluster;

import com.flipkart.krystal.krystex.NodeDefinition;
import com.google.common.collect.ImmutableMap;

public final class DefaultNodeCluster<T> implements NodeCluster<T> {

  private final ImmutableMap<String, NodeCluster<Object>> dependencies;
  private final ImmutableMap<String, ImmutableMap<String, NodeDefinition<Object>>>
      dependencyInputProviders;
  private final NodeDefinition<T> logicNode;

  public DefaultNodeCluster(
      ImmutableMap<String, NodeCluster<Object>> dependencies,
      ImmutableMap<String, ImmutableMap<String, NodeDefinition<Object>>> dependencyInputProviders,
      NodeDefinition<T> logicNode) {
    this.dependencies = dependencies;
    this.dependencyInputProviders = dependencyInputProviders;
    this.logicNode = logicNode;
  }

  @Override
  public ImmutableMap<String, NodeCluster<Object>> getDependencies() {
    return dependencies;
  }

  @Override
  public ImmutableMap<String, ImmutableMap<String, NodeDefinition<Object>>>
      dependencyInputProviders() {
    return dependencyInputProviders;
  }

  @Override
  public NodeDefinition<T> logicNode() {
    return logicNode;
  }
}

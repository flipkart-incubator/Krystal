package com.flipkart.krystal.krystex.nodecluster;

import com.flipkart.krystal.krystex.NodeDefinition;
import com.google.common.collect.ImmutableMap;

public sealed interface NodeCluster<T> permits DefaultNodeCluster {
  ImmutableMap<String, NodeCluster<Object>> getDependencies();

  /**
   * @return A mapping from {dependency name -> {dependency input name -> provider node}} for this
   *     Node Cluster
   */
  ImmutableMap<String, ImmutableMap<String, NodeDefinition<Object>>> dependencyInputProviders();

  NodeDefinition<T> logicNode();
}

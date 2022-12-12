package com.flipkart.krystal.krystex.nodecluster;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NodeClusterRegistry {

  private final Map<String, NodeCluster<Object>> nodeClusters = new LinkedHashMap<>();

  public NodeClusterRegistry() {
  }

}

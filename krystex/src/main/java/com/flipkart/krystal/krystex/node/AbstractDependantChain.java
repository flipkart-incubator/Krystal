package com.flipkart.krystal.krystex.node;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract sealed class AbstractDependantChain implements DependantChain
    permits DefaultDependantChain, DependantChainStart {

  private final Map<NodeId, Map<String, DependantChain>> dependenciesInternPool =
      new LinkedHashMap<>();

  @Override
  public DependantChain extend(NodeId nodeId, String dependencyName) {
    return dependenciesInternPool
        .computeIfAbsent(nodeId, _n -> new LinkedHashMap<>())
        .computeIfAbsent(
            dependencyName, depName -> new DefaultDependantChain(nodeId, depName, this));
  }
}

package com.flipkart.krystal.krystex.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract sealed class AbstractDependantChain implements DependantChain
    permits DefaultDependantChain, DependantChainStart {

  private final Map<NodeId, Map<String, DependantChain>> dependenciesInternPool =
      new ConcurrentHashMap<>();

  @Override
  public DependantChain extend(NodeId nodeId, String dependencyName) {
    return dependenciesInternPool
        .computeIfAbsent(nodeId, _n -> new ConcurrentHashMap<>())
        .computeIfAbsent(
            dependencyName, depName -> new DefaultDependantChain(nodeId, depName, this));
  }
}

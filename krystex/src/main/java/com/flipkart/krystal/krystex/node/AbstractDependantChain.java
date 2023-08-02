package com.flipkart.krystal.krystex.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public abstract sealed class AbstractDependantChain implements DependantChain
    permits DefaultDependantChain, DependantChainStart {

  @EqualsAndHashCode.Exclude @ToString.Exclude
  private final Map<NodeId, ConcurrentHashMap<String, DependantChain>> dependenciesInternPool =
      new ConcurrentHashMap<>();

  @Override
  public DependantChain extend(NodeId nodeId, String dependencyName) {
    return dependenciesInternPool
        .computeIfAbsent(nodeId, _n -> new ConcurrentHashMap<>())
        .computeIfAbsent(
            dependencyName, depName -> new DefaultDependantChain(nodeId, depName, this));
  }
}

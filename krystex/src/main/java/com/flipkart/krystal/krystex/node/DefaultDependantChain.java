package com.flipkart.krystal.krystex.node;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;

@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public final class DefaultDependantChain implements DependantChain {
  private final NodeId nodeId;
  private final String dependencyName;
  private final DependantChain dependantChain;

  DefaultDependantChain(NodeId nodeId, String dependencyName, DependantChain dependantChain) {
    this.nodeId = nodeId;
    this.dependencyName = dependencyName;
    this.dependantChain = dependantChain;
  }

  @Override
  public boolean contains(NodeId nodeId) {
    return this.nodeId.equals(nodeId) || dependantChain.contains(nodeId);
  }

  public NodeId nodeId() {
    return nodeId;
  }

  public String dependencyName() {
    return dependencyName;
  }

  public DependantChain dependantChain() {
    return dependantChain;
  }

  @Override
  public String toString() {
    return "%s.%s:%s".formatted(dependantChain, nodeId.value(), dependencyName());
  }
}

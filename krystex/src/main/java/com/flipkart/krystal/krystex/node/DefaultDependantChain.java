package com.flipkart.krystal.krystex.node;

public final class DefaultDependantChain extends AbstractDependantChain {
  private final NodeId nodeId;
  private final String dependencyName;
  private final DependantChain dependantChain;
  private int _hashCodeCache;

  DefaultDependantChain(NodeId nodeId, String dependencyName, DependantChain dependantChain) {
    this.nodeId = nodeId;
    this.dependencyName = dependencyName;
    this.dependantChain = dependantChain;
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
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    if (this._hashCodeCache == 0) {
      this._hashCodeCache = super.hashCode();
    }
    return this._hashCodeCache;
  }

  @Override
  public String toString() {
    return "%s:%s[%s]".formatted(dependantChain, nodeId.value(), dependencyName());
  }
}

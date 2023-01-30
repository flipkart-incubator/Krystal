package com.flipkart.krystal.krystex.node;

import java.util.Objects;

public record DependantChainFirstNode(NodeId nodeId, String dependencyName)
    implements DependantChain {
  public DependantChain dependantChain() {
    return DependantChainStart.instance();
  }

  @Override
  public String toString() {
    return "%s>%s:%s".formatted(dependantChain().toString(), nodeId().value(), dependencyName());
  }

  @Override
  public boolean contains(NodeId nodeId) {
    return Objects.equals(this.nodeId, nodeId);
  }
}

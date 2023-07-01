package com.flipkart.krystal.krystex.node;

public sealed interface DependantChain permits DefaultDependantChain, DependantChainStart {

  /**
   * @return {@code true} if the given nodeId is part of this DependantChain. {@code false}
   *     otherwise.
   */
  boolean contains(NodeId nodeId);

  static DependantChain start(NodeId nodeId, String dependencyName) {
    return new DefaultDependantChain(nodeId, dependencyName, DependantChainStart.instance());
  }

  default DependantChain extend(NodeId nodeId, String dependencyName) {
    return new DefaultDependantChain(nodeId, dependencyName, this);
  }
}

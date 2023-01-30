package com.flipkart.krystal.krystex.node;

import java.util.Optional;

public sealed interface DependantChain
    permits DefaultDependantChain, DependantChainFirstNode, DependantChainStart {

  /**
   * @return {@code true} if the given nodeId is part of this DependantChain. {@code false}
   *     otherwise.
   */
  boolean contains(NodeId nodeId);

  /**
   * Returns a new {@link DependantChain} representing the given strings which are passed in trigger
   * order (from [Start] to immediate dependant.)
   *
   * @param firstNodeId The first node in the DependantChain
   * @param firstDependencyName The dependency name of the first node in the DependencyChain
   * @param subsequentDependencyNames an array of strings representing the dependency names in the
   *     DependantChain in trigger order
   */
  static DependantChain fromTriggerOrder(
      NodeId firstNodeId, String firstDependencyName, String... subsequentDependencyNames) {
    DependantChain depChain = new DependantChainFirstNode(firstNodeId, firstDependencyName);
    for (int i = 0; i < subsequentDependencyNames.length; i++) {
      depChain = new DefaultDependantChain(subsequentDependencyNames[i], depChain);
    }
    return depChain;
  }

  static DependantChain from(NodeId nodeId, String dependencyName, DependantChain dependantChain) {
    if (dependantChain instanceof DependantChainStart) {
      return new DependantChainFirstNode(nodeId, dependencyName);
    } else {
      return new DefaultDependantChain(Optional.of(nodeId), dependencyName, dependantChain);
    }
  }
}

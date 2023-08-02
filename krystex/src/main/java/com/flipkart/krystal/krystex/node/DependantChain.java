package com.flipkart.krystal.krystex.node;

public sealed interface DependantChain permits AbstractDependantChain {

  DependantChain extend(NodeId nodeId, String dependencyName);
}

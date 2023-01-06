package com.flipkart.krystal.krystex.node;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

abstract class LogicDefinition {

  private final NodeLogicId nodeLogicId;
  private final ImmutableSet<String> inputNames;

  LogicDefinition(NodeLogicId nodeLogicId, Set<String> inputs) {
    this.nodeLogicId = nodeLogicId;
    this.inputNames = ImmutableSet.copyOf(inputs);
  }

  public NodeLogicId nodeLogicId() {
    return nodeLogicId;
  }

  public ImmutableSet<String> inputNames() {
    return inputNames;
  }
}

package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.logic.LogicTag;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

abstract sealed class LogicDefinition permits MainLogicDefinition, ResolverLogicDefinition {

  private final NodeLogicId nodeLogicId;
  private final ImmutableSet<String> inputNames;
  private final ImmutableMap<String, LogicTag> logicTags;

  LogicDefinition(
      NodeLogicId nodeLogicId, Set<String> inputs, ImmutableMap<String, LogicTag> logicTags) {
    this.nodeLogicId = nodeLogicId;
    this.inputNames = ImmutableSet.copyOf(inputs);
    this.logicTags = logicTags;
  }

  public NodeLogicId nodeLogicId() {
    return nodeLogicId;
  }

  public ImmutableSet<String> inputNames() {
    return inputNames;
  }

  public ImmutableMap<String, LogicTag> logicTags() {
    return logicTags;
  }
}

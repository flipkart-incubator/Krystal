package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.LogicTag;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public abstract sealed class LogicDefinition<L extends Logic>
    permits MainLogicDefinition, ResolverLogicDefinition {

  private final NodeLogicId nodeLogicId;
  private final ImmutableSet<String> inputNames;
  private final ImmutableMap<String, LogicTag> logicTags;
  private final L logic;

  LogicDefinition(
      NodeLogicId nodeLogicId,
      Set<String> inputs,
      ImmutableMap<String, LogicTag> logicTags,
      L logic) {
    this.nodeLogicId = nodeLogicId;
    this.inputNames = ImmutableSet.copyOf(inputs);
    this.logicTags = logicTags;
    this.logic = logic;
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

  public L logic() {
    return logic;
  }
  ;
}

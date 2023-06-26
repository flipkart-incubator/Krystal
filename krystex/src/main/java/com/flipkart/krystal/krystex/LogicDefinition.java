package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverLogicDefinition;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public abstract sealed class LogicDefinition<L extends Logic>
    permits MainLogicDefinition, MultiResolverDefinition, ResolverLogicDefinition {

  private final NodeLogicId nodeLogicId;
  private final ImmutableSet<String> inputNames;
  private final ImmutableMap<String, Tag> logicTags;
  private final L logic;

  protected LogicDefinition(
      NodeLogicId nodeLogicId, Set<String> inputs, ImmutableMap<String, Tag> logicTags, L logic) {
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

  public ImmutableMap<String, Tag> logicTags() {
    return logicTags;
  }

  public L logic() {
    return logic;
  }
}

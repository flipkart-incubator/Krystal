package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverLogicDefinition;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public abstract sealed class LogicDefinition<L extends Logic>
    permits OutputLogicDefinition, MultiResolverDefinition, ResolverLogicDefinition {

  private final KryonLogicId kryonLogicId;
  private final ImmutableSet<String> inputNames;
  private final ImmutableMap<Object, Tag> logicTags;
  private final L logic;

  protected LogicDefinition(
      KryonLogicId kryonLogicId, Set<String> inputs, ImmutableMap<Object, Tag> logicTags, L logic) {
    this.kryonLogicId = kryonLogicId;
    this.inputNames = ImmutableSet.copyOf(inputs);
    this.logicTags = logicTags;
    this.logic = logic;
  }

  public KryonLogicId kryonLogicId() {
    return kryonLogicId;
  }

  public ImmutableSet<String> inputNames() {
    return inputNames;
  }

  public ImmutableMap<Object, Tag> logicTags() {
    return logicTags;
  }

  public L logic() {
    return logic;
  }
}

package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class LogicDefinition<L extends Logic> {

  private final KryonLogicId kryonLogicId;
  private final ImmutableSet<Integer> inputIds;
  private final ImmutableMap<Object, Tag> logicTags;
  private final L logic;

  public LogicDefinition(KryonLogicId kryonLogicId, L logic) {
    this.kryonLogicId = kryonLogicId;
    this.inputIds = ImmutableSet.of();
    this.logicTags = ImmutableMap.of();
    this.logic = logic;
  }

  public LogicDefinition(
      KryonLogicId kryonLogicId,
      Set<Integer> inputs,
      ImmutableMap<Object, Tag> logicTags,
      L logic) {
    this.kryonLogicId = kryonLogicId;
    this.inputIds = ImmutableSet.copyOf(inputs);
    this.logicTags = logicTags;
    this.logic = logic;
  }

  public KryonLogicId kryonLogicId() {
    return kryonLogicId;
  }

  public ImmutableSet<Integer> inputNames() {
    return inputIds;
  }

  public ImmutableMap<Object, Tag> logicTags() {
    return logicTags;
  }

  public L logic() {
    return logic;
  }
}

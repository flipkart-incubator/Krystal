package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class LogicDefinition<L extends Logic> {

  private final KryonLogicId kryonLogicId;
  private final ImmutableSet<Integer> inputIds;
  private final ElementTags tags;
  private final L logic;

  public LogicDefinition(KryonLogicId kryonLogicId, L logic) {
    this(kryonLogicId, ImmutableSet.of(), ElementTags.emptyTags(), logic);
  }

  public LogicDefinition(
      KryonLogicId kryonLogicId, Set<Integer> inputs, ElementTags tags, L logic) {
    this.kryonLogicId = kryonLogicId;
    this.inputIds = ImmutableSet.copyOf(inputs);
    this.tags = tags;
    this.logic = logic;
  }

  public KryonLogicId kryonLogicId() {
    return kryonLogicId;
  }

  public ImmutableSet<Integer> inputIds() {
    return inputIds;
  }

  public ElementTags tags() {
    return tags;
  }

  public L logic() {
    return logic;
  }
}

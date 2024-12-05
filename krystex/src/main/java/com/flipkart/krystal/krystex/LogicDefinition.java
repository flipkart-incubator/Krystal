package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverLogicDefinition;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public abstract sealed class LogicDefinition<L extends Logic>
    permits OutputLogicDefinition, MultiResolverDefinition, ResolverLogicDefinition {

  private final KryonLogicId kryonLogicId;
  private final ImmutableSet<String> inputNames;
  private final ElementTags tags;
  private final L logic;

  protected LogicDefinition(
      KryonLogicId kryonLogicId, Set<String> inputs, ElementTags tags, L logic) {
    this.kryonLogicId = kryonLogicId;
    this.inputNames = ImmutableSet.copyOf(inputs);
    this.tags = tags;
    this.logic = logic;
  }

  public KryonLogicId kryonLogicId() {
    return kryonLogicId;
  }

  public ImmutableSet<String> inputNames() {
    return inputNames;
  }

  public ElementTags tags() {
    return tags;
  }

  public L logic() {
    return logic;
  }
}

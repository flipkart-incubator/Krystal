package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public final class ComputeLogicDefinition<T> extends OutputLogicDefinition<T> {

  public ComputeLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<Integer> inputs,
      OutputLogic<T> outputLogic,
      ImmutableMap<Object, Tag> logicTags) {
    super(kryonLogicId, inputs, logicTags, outputLogic);
  }
}

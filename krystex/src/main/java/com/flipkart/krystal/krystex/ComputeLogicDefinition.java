package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public final class ComputeLogicDefinition<T> extends MainLogicDefinition<T> {

  public ComputeLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<String> inputs,
      MainLogic<T> mainLogic,
      ImmutableMap<String, Tag> logicTags) {
    super(kryonLogicId, inputs, logicTags, mainLogic);
  }
}

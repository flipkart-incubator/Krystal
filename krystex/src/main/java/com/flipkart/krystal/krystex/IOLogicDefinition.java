package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public final class IOLogicDefinition<T> extends MainLogicDefinition<T> {

  public IOLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<String> inputs,
      MainLogic<T> mainLogic,
      ImmutableMap<Object, Tag> logicTags) {
    super(kryonLogicId, inputs, logicTags, mainLogic);
  }
}

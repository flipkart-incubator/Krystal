package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.tags.ElementTags;
import java.util.Set;

public final class IOLogicDefinition<T> extends OutputLogicDefinition<T> {

  public IOLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<Integer> inputs,
      OutputLogic<T> outputLogic,
      ElementTags tags) {
    super(kryonLogicId, inputs, tags, outputLogic);
  }
}

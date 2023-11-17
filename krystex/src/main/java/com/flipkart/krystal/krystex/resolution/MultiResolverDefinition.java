package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public final class MultiResolverDefinition extends LogicDefinition<MultiResolver> {

  public MultiResolverDefinition(
      KryonLogicId kryonLogicId,
      Set<String> inputs,
      MultiResolver logic,
      ImmutableMap<Object, Tag> logicTags) {
    super(kryonLogicId, inputs, logicTags, logic);
  }
}

package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public final class ResolverLogicDefinition extends LogicDefinition<ResolverLogic> {

  public ResolverLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<String> inputNames,
      ResolverLogic resolverLogic,
      ImmutableMap<Object, Tag> logicTags) {
    super(kryonLogicId, inputNames, logicTags, resolverLogic);
  }

  public ResolverCommand resolve(Inputs inputs) {
    return logic().resolve(inputs);
  }
}

package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.logic.LogicTag;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public final class ResolverLogicDefinition extends LogicDefinition<ResolverLogic> {

  public ResolverLogicDefinition(
      NodeLogicId nodeLogicId,
      Set<String> inputNames,
      ResolverLogic resolverLogic,
      ImmutableMap<String, LogicTag> logicTags) {
    super(nodeLogicId, inputNames, logicTags, resolverLogic);
  }

  public ResolverCommand resolve(Inputs inputs) {
    return logic().resolve(inputs);
  }
}

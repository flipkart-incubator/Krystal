package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.logic.LogicTag;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public final class ResolverLogicDefinition extends LogicDefinition {

  private final ResolverLogic resolverLogic;

  public ResolverLogicDefinition(
      NodeLogicId nodeLogicId,
      Set<String> inputNames,
      ResolverLogic nodeLogic,
      ImmutableMap<String, LogicTag> logicTags) {
    super(nodeLogicId, inputNames, logicTags);
    this.resolverLogic = nodeLogic;
  }

  public ResolverCommand resolve(Inputs inputs) {
    return resolverLogic.resolve(inputs);
  }
}

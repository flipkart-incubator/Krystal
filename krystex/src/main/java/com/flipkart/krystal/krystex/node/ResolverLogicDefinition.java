package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import java.util.Set;

public final class ResolverLogicDefinition extends LogicDefinition {

  private final ResolverNodeLogic resolverLogic;

  public ResolverLogicDefinition(
      NodeLogicId nodeLogicId, Set<String> inputNames, ResolverNodeLogic nodeLogic) {
    super(nodeLogicId, inputNames);
    this.resolverLogic = nodeLogic;
  }

  ResolverCommand resolve(Inputs nodeInputs) {
    return resolverLogic.resolve(nodeInputs);
  }
}

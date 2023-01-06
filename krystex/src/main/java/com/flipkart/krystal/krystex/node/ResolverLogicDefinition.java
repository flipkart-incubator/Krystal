package com.flipkart.krystal.krystex.node;

import java.util.Set;

public final class ResolverLogicDefinition extends LogicDefinition {

  private final ResolverNodeLogic resolverLogic;

  public ResolverLogicDefinition(
      NodeLogicId nodeLogicId, Set<String> inputNames, ResolverNodeLogic nodeLogic) {
    super(nodeLogicId, inputNames);
    this.resolverLogic = nodeLogic;
  }

  ResolverCommand resolve(NodeInputs nodeInputs) {
    return resolverLogic.resolve(nodeInputs);
  }
}

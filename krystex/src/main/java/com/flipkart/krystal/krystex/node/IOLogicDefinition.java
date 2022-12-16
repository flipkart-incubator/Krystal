package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.node.NodeDecorator;
import com.flipkart.krystal.krystex.node.NodeLogic;
import com.flipkart.krystal.krystex.node.NodeLogicDefinition;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.function.Supplier;

public final class IOLogicDefinition<T> extends NodeLogicDefinition<T> {

  private final NodeLogic<T> nodeLogic;
  /**
   * The group type by which input modulators are grouped. All nodes with the same group id for this
   * groupType will have the same InputModulation decorator.
   */
  private String inputModulatorGroupType;

  private Supplier<NodeDecorator<T>> inputModulationDecorator;

  IOLogicDefinition(
      NodeLogicId nodeLogicId,
      Set<String> inputs,
      ImmutableMap<String, String> groupMemberships,
      NodeLogic<T> nodeLogic) {
    super(nodeLogicId, inputs);
    this.nodeLogic = nodeLogic;
  }

  @Override
  public NodeLogic<T> logic() {
    return nodeLogic;
  }
}

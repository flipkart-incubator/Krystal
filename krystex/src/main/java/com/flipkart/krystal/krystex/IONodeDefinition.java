package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class IONodeDefinition<T> extends NodeDefinition<T> {

  private final NodeLogic<T> nodeLogic;
  /**
   * The group type by which input modulators are grouped. All nodes with the same group id for this
   * groupType will have the same InputModulation decorator.
   */
  private String inputModulatorGroupType;

  private Supplier<NodeDecorator<T>> inputModulationDecorator;

  IONodeDefinition(
      String nodeId,
      Set<String> inputs,
      Map<String, String> inputProviders,
      ImmutableMap<String, String> groupMemberships,
      NodeLogic<T> nodeLogic) {
    super(nodeId, inputs, inputProviders, groupMemberships);
    this.nodeLogic = nodeLogic;
  }

  @Override
  public NodeLogic<T> logic() {
    return nodeLogic;
  }
}

package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class IONodeDefinition<T> extends NodeDefinition<T> {

  private final NodeLogic<T> nodeLogic;
  private NodeDecorator<T> inputModulationDecorator;

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
  public final NodeLogic<T> logic() {
    return getInputModulationDecorator()
        .map(nd -> nd.decorateLogic(this, nodeLogic))
        .orElse(nodeLogic);
  }

  public void setInputModulationDecorator(NodeDecorator<T> inputModulationAdaptor) {
    this.inputModulationDecorator = inputModulationAdaptor;
  }

  private Optional<NodeDecorator<T>> getInputModulationDecorator() {
    return Optional.ofNullable(inputModulationDecorator);
  }
}

package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

final class VajramDefinition {

  @Getter private final Vajram<?> vajram;
  @Getter private final ImmutableCollection<InputResolver> inputResolvers;
  /** A mapping of input resolvers to node names */
  // TODO populate this map
  private final Map<String, String> nodesForInputs = new HashMap<>();

  VajramDefinition(Vajram<?> vajram) {
    this.vajram = vajram;
    this.inputResolvers = ImmutableList.of();
  }

  public ImmutableMap<String, String> getNodesForInputs() {
    return ImmutableMap.copyOf(nodesForInputs);
  }

  public void addNodeForInput(String dependencyName, String providerNodeId) {
    nodesForInputs.put(dependencyName, providerNodeId);
  }
}

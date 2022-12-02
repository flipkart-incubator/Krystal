package com.flipkart.krystal.krystex;

import java.util.Map;
import java.util.Set;

public abstract non-sealed class BlockingNodeDefinition<T> extends NodeDefinition<T> {

  BlockingNodeDefinition(String nodeId, Set<String> dependencies, Map<String, String> dependencyProviders, Set<String> inputs) {
    super(nodeId, dependencies, dependencyProviders, inputs);
  }

  public abstract InputModulator getInputModulator();
}

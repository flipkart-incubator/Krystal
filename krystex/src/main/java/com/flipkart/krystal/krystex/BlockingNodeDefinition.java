package com.flipkart.krystal.krystex;

import java.util.Map;
import java.util.Set;

public abstract non-sealed class BlockingNodeDefinition<T> extends NodeDefinition<T> {

  BlockingNodeDefinition(String nodeId, Set<String> inputs, Map<String, String> inputProviders) {
    super(nodeId, inputs, inputProviders);
  }

  public abstract InputModulator getInputModulator();
}

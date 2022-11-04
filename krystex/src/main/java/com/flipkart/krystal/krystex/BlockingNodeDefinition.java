package com.flipkart.krystal.krystex;

import java.util.Map;

public abstract non-sealed class BlockingNodeDefinition<T> extends NodeDefinition<T> {

  BlockingNodeDefinition(String nodeId, Map<String, String> inputs) {
    super(nodeId, inputs);
  }

  public abstract InputModulator getInputModulator();
}

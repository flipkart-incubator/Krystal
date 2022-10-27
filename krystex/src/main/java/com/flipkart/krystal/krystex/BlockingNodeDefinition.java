package com.flipkart.krystal.krystex;

import java.util.Set;

public abstract non-sealed class BlockingNodeDefinition<T> extends NodeDefinition<T> {

  public BlockingNodeDefinition(String nodeId, Set<String> inputs) {
    super(nodeId, inputs);
  }

  public abstract InputModulator getInputModulator();
}

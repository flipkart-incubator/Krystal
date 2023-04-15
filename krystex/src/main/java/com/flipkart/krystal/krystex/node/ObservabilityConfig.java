package com.flipkart.krystal.krystex.node;

public record ObservabilityConfig(
    boolean recordNodeExecutionTimes, boolean recordNodeInputsAndOutputs) {
  private static final ObservabilityConfig NO_OP = new ObservabilityConfig(false, false);

  public static ObservabilityConfig noOp() {
    return NO_OP;
  }
}

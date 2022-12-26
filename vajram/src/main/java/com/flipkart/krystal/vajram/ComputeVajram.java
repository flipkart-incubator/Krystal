package com.flipkart.krystal.vajram;

public abstract non-sealed class ComputeVajram<T> extends AbstractVajram<T> {

  @Override
  public final boolean isIOVajram() {
    return false;
  }

  public abstract T executeCompute(ExecutionContextMap executionContext);
}

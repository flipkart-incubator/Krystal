package com.flipkart.krystal.vajram;

public abstract non-sealed class ComputeVajram<T> extends AbstractVajram<T> {

  @Override
  public final boolean isIOVajram() {
    return false;
  }

  public T executeCompute(ExecutionContextMap executionContext) {
    throw new UnsupportedOperationException(
        "executeCompute method should be implemented by a ComputeVajram");
  }
}

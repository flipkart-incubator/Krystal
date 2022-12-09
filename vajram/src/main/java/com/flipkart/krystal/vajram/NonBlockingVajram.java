package com.flipkart.krystal.vajram;

public abstract non-sealed class NonBlockingVajram<T> extends AbstractVajram<T> {

  @Override
  public final boolean isIOVajram() {
    return false;
  }

  public abstract T executeNonBlocking(ExecutionContextMap executionContext);
}

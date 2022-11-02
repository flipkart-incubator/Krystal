package com.flipkart.krystal.vajram;

public abstract sealed class BlockingVajram<T> extends AbstractVajram<T>
    permits DefaultModulatedBlockingVajram, UnmodulatedAsyncVajram {

  @Override
  public final boolean isBlockingVajram() {
    return true;
  }
}

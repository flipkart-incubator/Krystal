package com.flipkart.krystal.vajram;

public sealed abstract class BlockingVajram implements Vajram permits
    DefaultModulatedBlockingVajram,
    UnmodulatedAsyncVajram {

  @Override
  public final boolean isBlockingVajram() {
    return true;
  }
}

package com.flipkart.krystal.vajram;

public abstract non-sealed class NonBlockingVajram implements Vajram{

  @Override
  public final boolean isBlockingVajram() {
    return false;
  }
}
package com.flipkart.krystal.vajram.lang.rust.ast;

/** Completion contract of a Vajram execution. */
public enum Completion {
  NOW,
  SOON,
  LATER;

  public boolean isAsync() {
    return this != NOW;
  }
}

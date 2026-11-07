package com.flipkart.krystal.keka.rust.ast;

/** Completion contract of a Vajram execution. */
public enum Completion {
  NOW,
  SOON,
  LATER;

  public boolean isAsync() {
    return this != NOW;
  }
}

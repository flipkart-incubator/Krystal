package com.flipkart.krystal.core;

// @jdk.internal.ValueBased
public record VajramID(String id) {

  public static VajramID vajramID(String id) {
    return new VajramID(id);
  }

  @Override
  public String toString() {
    return "v<%s>".formatted(id());
  }
}

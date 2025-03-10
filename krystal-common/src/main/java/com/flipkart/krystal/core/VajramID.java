package com.flipkart.krystal.core;

// @jdk.internal.ValueBased
public record VajramID(String vajramId) {

  public static VajramID vajramID(String id) {
    return new VajramID(id);
  }

  @Override
  public String toString() {
    return "v<%s>".formatted(vajramId());
  }

  public String value() {
    return vajramId;
  }
}

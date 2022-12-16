package com.flipkart.krystal.krystex.node;

public record NodeLogicId(String asString) {

  @Override
  public String toString() {
    return "n<%s>".formatted(asString());
  }
}

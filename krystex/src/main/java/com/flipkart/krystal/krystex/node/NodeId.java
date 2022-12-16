package com.flipkart.krystal.krystex.node;

public record NodeId(String value) {

  @Override
  public String toString() {
    return "n<%s>".formatted(value());
  }
}

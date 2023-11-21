package com.flipkart.krystal.krystex.kryon;

public record KryonId(String value) {

  @Override
  public String toString() {
    return "k<%s>".formatted(value());
  }
}

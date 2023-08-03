package com.flipkart.krystal.krystex.kryon;

public record KryonLogicId(KryonId kryonId, String value) {

  @Override
  public String toString() {
    return "l<%s>".formatted(value());
  }
}

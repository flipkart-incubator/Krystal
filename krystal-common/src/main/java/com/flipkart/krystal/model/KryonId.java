package com.flipkart.krystal.model;

public record KryonId(String value) {

  @Override
  public String toString() {
    return "n<%s>".formatted(value());
  }
}

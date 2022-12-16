package com.flipkart.krystal.krystex;

public record RequestId(String asString) {

  public RequestId append(Object suffix) {
    return new RequestId("%s:%s".formatted(asString, suffix));
  }
}

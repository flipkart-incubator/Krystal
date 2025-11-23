package com.flipkart.krystal.krystex.kryon;

public record DirectResponse() implements KryonCommandResponse {
  private static final DirectResponse INSTANCE = new DirectResponse();

  @SuppressWarnings("unchecked")
  public static <R extends KryonCommandResponse> R instance() {
    return (R) INSTANCE;
  }
}

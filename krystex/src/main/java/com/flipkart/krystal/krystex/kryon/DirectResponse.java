package com.flipkart.krystal.krystex.kryon;

public record DirectResponse() implements KryonCommandResponse {
  public static final DirectResponse INSTANCE = new DirectResponse();
}

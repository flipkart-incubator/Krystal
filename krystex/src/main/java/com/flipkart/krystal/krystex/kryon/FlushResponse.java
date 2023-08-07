package com.flipkart.krystal.krystex.kryon;

@SuppressWarnings("Singleton")
final class FlushResponse implements KryonResponse {

  private static final FlushResponse INSTANCE = new FlushResponse();

  public static FlushResponse getInstance() {
    return INSTANCE;
  }

  private FlushResponse() {}
}

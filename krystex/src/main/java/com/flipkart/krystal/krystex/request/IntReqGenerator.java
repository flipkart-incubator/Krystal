package com.flipkart.krystal.krystex.request;

import java.util.function.Supplier;

public class IntReqGenerator implements RequestIdGenerator {
  private int nextCounter = 0;

  @Override
  public InvocationId newSubRequest(InvocationId parent, Supplier<String> suffix) {
    return new InvocationId(nextCounter++);
  }

  @Override
  public InvocationId newRequest(Object seed) {
    return new InvocationId(nextCounter++);
  }
}

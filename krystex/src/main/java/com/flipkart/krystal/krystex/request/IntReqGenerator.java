package com.flipkart.krystal.krystex.request;

import java.util.function.Supplier;

public class IntReqGenerator implements RequestIdGenerator {
  private int nextCounter = 0;

  @Override
  public RequestId newSubRequest(RequestId parent, Supplier<String> suffix) {
    return new RequestId(nextCounter++, parent.originatedFrom());
  }

  @Override
  public RequestId newRequest(Object seed) {
    return new RequestId(nextCounter++);
  }
}

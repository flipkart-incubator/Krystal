package com.flipkart.krystal.krystex.request;

import java.util.function.Supplier;

public class StringReqGenerator implements RequestIdGenerator {

  @Override
  public RequestId newSubRequest(RequestId parent, Supplier<String> suffixSupplier) {
    return new RequestId("%s:%s".formatted(parent.id(), suffixSupplier.get()));
  }

  @Override
  public RequestId newRequest(Object seed) {
    return new RequestId(seed);
  }
}

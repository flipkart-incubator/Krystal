package com.flipkart.krystal.krystex.request;

import java.util.function.Supplier;

public class StringReqGenerator implements RequestIdGenerator {

  @Override
  public InvocationId newSubRequest(InvocationId parent, Supplier<String> suffixSupplier) {
    return new InvocationId("%s:%s".formatted(parent.id(), suffixSupplier.get()));
  }

  @Override
  public InvocationId newRequest(Supplier<Object> seed) {
    return new InvocationId(seed.get());
  }
}

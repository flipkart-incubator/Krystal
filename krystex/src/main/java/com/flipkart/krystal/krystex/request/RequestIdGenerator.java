package com.flipkart.krystal.krystex.request;

import java.util.function.Supplier;

public interface RequestIdGenerator {
  InvocationId newSubRequest(InvocationId parent, Supplier<String> suffix);

  InvocationId newRequest(Supplier<Object> seed);
}

package com.flipkart.krystal.krystex.request;

import java.util.function.Supplier;

public interface RequestIdGenerator {
  RequestId newSubRequest(RequestId parent, Supplier<String> suffix);

  RequestId newRequest(Object seed);
}

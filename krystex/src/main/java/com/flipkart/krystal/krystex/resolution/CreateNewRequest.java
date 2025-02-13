package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.Logic;

@FunctionalInterface
public non-sealed interface CreateNewRequest extends Logic {
  ImmutableRequest.Builder<?> newRequestBuilder();
}

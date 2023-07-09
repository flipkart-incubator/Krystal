package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.google.common.collect.ImmutableMap;

public record BatchNodeResponse(ImmutableMap<Inputs, ValueOrError<Object>> response)
    implements NodeResponse {

  public BatchNodeResponse() {
    this(ImmutableMap.of());
  }
}

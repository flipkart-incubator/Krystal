package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;

public record BatchResponse(ImmutableMap<RequestId, ValueOrError<Object>> responses)
    implements NodeResponse {

  public BatchResponse() {
    this(ImmutableMap.of());
  }
}

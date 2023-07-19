package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;

public record BatchNodeResponse(ImmutableMap<RequestId, ValueOrError<Object>> responses)
    implements NodeResponse {

  public BatchNodeResponse() {
    this(ImmutableMap.of());
  }
}

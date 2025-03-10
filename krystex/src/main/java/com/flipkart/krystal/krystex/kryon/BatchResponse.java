package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;

public record BatchResponse(ImmutableMap<RequestId, Errable<Object>> responses)
    implements KryonResponse {

  private static final BatchResponse EMPTY = new BatchResponse(ImmutableMap.of());

  public static BatchResponse empty() {
    return EMPTY;
  }
}

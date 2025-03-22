package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.ImmutableMap;

public record BatchResponse(ImmutableMap<InvocationId, Errable<Object>> responses)
    implements KryonCommandResponse {

  private static final BatchResponse EMPTY = new BatchResponse(ImmutableMap.of());

  public static BatchResponse empty() {
    return EMPTY;
  }
}

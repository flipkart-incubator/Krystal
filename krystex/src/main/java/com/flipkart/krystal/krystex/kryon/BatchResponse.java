package com.flipkart.krystal.krystex.kryon;

import static java.util.Collections.unmodifiableMap;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public record BatchResponse(Map<InvocationId, Errable<Object>> responses)
    implements KryonCommandResponse {

  private static final BatchResponse EMPTY = new BatchResponse(ImmutableMap.of());

  public static BatchResponse empty() {
    return EMPTY;
  }

  public BatchResponse {
    responses = unmodifiableMap(responses);
  }
}

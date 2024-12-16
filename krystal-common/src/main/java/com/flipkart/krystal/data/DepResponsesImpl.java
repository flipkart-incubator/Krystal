package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.Errable.nil;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DepResponsesImpl<R extends Request<T>, T> implements DependencyResponses<R, T> {

  private final ImmutableList<RequestResponse<R, T>> requestResponses;

  private @MonotonicNonNull ImmutableMap<ImmutableRequest<T>, Errable<T>> responsesAsMap;

  public DepResponsesImpl(ImmutableList<RequestResponse<R, T>> requestResponses) {
    this.requestResponses = requestResponses;
  }

  private static final DepResponsesImpl<?, ?> EMPTY = new DepResponsesImpl<>(ImmutableList.of());

  @Override
  public ImmutableCollection<RequestResponse<R, T>> requestResponsePairs() {
    return requestResponses;
  }

  @Override
  public Errable<T> getForRequest(R request) {
    if (responsesAsMap == null) {
      responsesAsMap = asMap();
    }
    Errable<T> response = responsesAsMap.getOrDefault(request._build(), nil());
    return response;
  }

  private ImmutableMap<ImmutableRequest<T>, Errable<T>> asMap() {
    return requestResponsePairs().stream()
        .collect(toImmutableMap(t -> t.request()._build(), o -> o.response()));
  }

  @SuppressWarnings("unchecked")
  public static <R extends Request<T>, T> DepResponsesImpl<R, T> empty() {
    return (DepResponsesImpl<R, T>) EMPTY;
  }
}

package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.Errable.nil;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

@ToString(onlyExplicitlyIncluded = true)
public final class FanoutDepResponses<R extends Request<T>, T> implements DepResponse<R, T> {

  private static final FanoutDepResponses EMPTY = new FanoutDepResponses<>(ImmutableList.of());

  @SuppressWarnings("unchecked")
  public static <R extends Request<T>, T> FanoutDepResponses<R, T> empty() {
    return EMPTY;
  }

  @ToString.Include @Getter
  private final ImmutableCollection<RequestResponse<R, T>> requestResponsePairs;

  private @MonotonicNonNull ImmutableMap<ImmutableRequest<T>, Errable<@NonNull T>> responsesAsMap;

  public FanoutDepResponses(ImmutableList<RequestResponse<R, T>> requestResponsePairs) {
    this.requestResponsePairs = requestResponsePairs;
  }

  public Errable<@NonNull T> getForRequest(Request<T> request) {
    if (responsesAsMap == null) {
      responsesAsMap = asMap();
    }
    return responsesAsMap.getOrDefault(request._build(), nil());
  }

  private ImmutableMap<ImmutableRequest<T>, Errable<@NonNull T>> asMap() {
    return requestResponsePairs().stream()
        .collect(toImmutableMap(t -> t.request()._build(), o -> o.response()));
  }
}

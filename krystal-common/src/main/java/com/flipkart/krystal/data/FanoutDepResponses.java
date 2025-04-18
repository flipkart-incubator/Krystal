package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.Errable.nil;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * @param <R> The type of the request
 * @param <T> The response type of the dependency vajram
 */
@ToString(onlyExplicitlyIncluded = true)
public final class FanoutDepResponses<T, R extends Request<T>> implements DepResponse<T, R> {

  private static final FanoutDepResponses<?, ?> EMPTY =
      new FanoutDepResponses<>(ImmutableList.of());

  @SuppressWarnings("unchecked")
  public static <R extends Request<T>, T> FanoutDepResponses<T, R> empty() {
    return (FanoutDepResponses<T, R>) EMPTY;
  }

  @ToString.Include @Getter
  private final ImmutableCollection<RequestResponse<R, T>> requestResponsePairs;

  private @MonotonicNonNull ImmutableMap<ImmutableRequest<T>, Errable<T>> responsesAsMap;

  public FanoutDepResponses(ImmutableList<RequestResponse<R, T>> requestResponsePairs) {
    this.requestResponsePairs = requestResponsePairs;
  }

  public Errable<T> getForRequest(Request<T> request) {
    if (responsesAsMap == null) {
      responsesAsMap = asMap();
    }
    return responsesAsMap.getOrDefault(request._build(), nil());
  }

  private ImmutableMap<ImmutableRequest<T>, Errable<T>> asMap() {
    return requestResponsePairs().stream()
        .collect(toImmutableMap(t -> t.request()._build(), RequestResponse::response));
  }
}

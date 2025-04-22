package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.Errable.nil;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * @param <R> The type of the request
 * @param <T> The response type of the dependency vajram
 */
@ToString(onlyExplicitlyIncluded = true)
public final class FanoutDepResponses<R extends Request<T>, T> implements DepResponse<R, T> {

  private static final FanoutDepResponses<?, ?> EMPTY =
      new FanoutDepResponses<>(ImmutableList.of());

  @SuppressWarnings("unchecked")
  public static <R extends Request<T>, T> FanoutDepResponses<R, T> empty() {
    return (FanoutDepResponses<R, T>) EMPTY;
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

  public void forEach(BiConsumer<R, Errable<T>> action) {
    requestResponsePairs.forEach(
        requestResponse -> action.accept(requestResponse.request(), requestResponse.response()));
  }

  private ImmutableMap<ImmutableRequest<T>, Errable<T>> asMap() {
    return requestResponsePairs().stream()
        .collect(toImmutableMap(t -> t.request()._build(), RequestResponse::response));
  }
}

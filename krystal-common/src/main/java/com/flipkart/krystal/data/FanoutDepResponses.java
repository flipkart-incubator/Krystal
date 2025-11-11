package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.Errable.nil;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Response of a Fanout dependency
 *
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

  @ToString.Include @Getter private final List<RequestResponse<R, T>> requestResponsePairs;

  private @MonotonicNonNull Map<ImmutableRequest<T>, Errable<T>> responsesAsMap;
  private @MonotonicNonNull List<Errable<T>> responsesAsList;

  public FanoutDepResponses(List<RequestResponse<R, T>> requestResponsePairs) {
    this.requestResponsePairs = unmodifiableList(requestResponsePairs);
  }

  public void forEach(BiConsumer<R, Errable<T>> action) {
    requestResponsePairs.forEach(
        requestResponse -> action.accept(requestResponse.request(), requestResponse.response()));
  }

  public Errable<T> getForRequest(Request<T> request) {
    if (responsesAsMap == null) {
      Map<ImmutableRequest<T>, Errable<T>> map = new HashMap<>();
      for (RequestResponse<R, T> t : requestResponsePairs()) {
        map.put(t.request()._build(), t.response());
      }
      this.responsesAsMap = unmodifiableMap(map);
    }
    return responsesAsMap.getOrDefault(request._build(), nil());
  }

  public List<Errable<T>> responses() {
    if (responsesAsList == null) {
      List<Errable<T>> result = new ArrayList<>(requestResponsePairs().size());
      for (var requestResponsePair : requestResponsePairs()) {
        result.add(requestResponsePair.response());
      }
      this.responsesAsList = unmodifiableList(result);
    }
    return responsesAsList;
  }
}

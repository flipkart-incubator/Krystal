package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap.SimpleImmutableEntry;

public record Results<R extends Request<T>, T>(
    ImmutableList<RequestResponse<R, T>> requestResponses) implements Responses<R, T> {

  private static final Results<?, ?> EMPTY = new Results<>(ImmutableList.of());

  @Override
  public ImmutableMap<R, Errable<T>> asMap() {
    return ImmutableMap.copyOf(
        (Iterable<SimpleImmutableEntry<R, Errable<T>>>)
            this.requestResponses().stream()
                    .map(r -> new SimpleImmutableEntry<>(r.request(), r.response()))
                ::iterator);
  }

  public static <R extends Request<T>, T> Results<R, T> empty() {
    //noinspection unchecked
    return (Results<R, T>) EMPTY;
  }
}

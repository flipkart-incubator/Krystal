package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableList;

public record Results<R extends Request<T>, T>(ImmutableList<Response<R, T>> responses)
    implements Responses<R, T> {

  private static final Results<?, ?> EMPTY = new Results<>(ImmutableList.of());

  public static <R extends Request<T>, T> Results<R, T> empty() {
    //noinspection unchecked
    return (Results<R, T>) EMPTY;
  }
}

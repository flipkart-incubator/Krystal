package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;

public record Results<T>(ImmutableMap<Facets, ValueOrError<T>> values) implements FacetValue<T> {

  private static final Results<?> EMPTY = new Results<>(ImmutableMap.of());

  public static <T> Results<T> empty() {
    //noinspection unchecked
    return (Results<T>) EMPTY;
  }
}

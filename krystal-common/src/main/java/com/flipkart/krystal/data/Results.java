package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;

public record Results<T>(ImmutableMap<Facets, Errable<T>> values) implements FacetValue<T> {

  private static final Results<?> EMPTY = new Results<>(ImmutableMap.of());

  @SuppressWarnings("unchecked")
  public static <T> Results<T> empty() {
    return (Results<T>) EMPTY;
  }
}

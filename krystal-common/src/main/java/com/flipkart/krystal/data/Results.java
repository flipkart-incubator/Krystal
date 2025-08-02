package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public record Results<T>(Map<Facets, Errable<T>> values) implements FacetValue<T> {

  private static final Results<?> EMPTY = new Results<>(ImmutableMap.of());

  @SuppressWarnings("unchecked")
  public static <T> Results<T> empty() {
    return (Results<T>) EMPTY;
  }
}

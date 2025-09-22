package com.flipkart.krystal.core;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

// TODO:
//  Deprecate this class. Returning futures in maps is error prone (because of equality and
//  other requirements on the facet values) and is not optimal
public record OutputLogicExecutionResults<T>(
    ImmutableMap<? extends ImmutableFacetValues, CompletableFuture<@Nullable T>> results) {

  private static final OutputLogicExecutionResults<?> EMPTY =
      new OutputLogicExecutionResults<>(ImmutableMap.of());

  public OutputLogicExecutionResults<T> withResults(
      Map<? extends ImmutableFacetValues, CompletableFuture<@Nullable T>> results) {
    return new OutputLogicExecutionResults<>(ImmutableMap.copyOf(results));
  }

  @SuppressWarnings("unchecked")
  public static <T> OutputLogicExecutionResults<T> empty() {
    return (OutputLogicExecutionResults<T>) EMPTY;
  }
}

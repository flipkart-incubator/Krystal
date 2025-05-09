package com.flipkart.krystal.core;

import com.flipkart.krystal.data.FacetValues;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OutputLogicExecutionResults<T>(
    ImmutableMap<? extends FacetValues, CompletableFuture<@Nullable T>> results) {

  private static final OutputLogicExecutionResults<?> EMPTY =
      new OutputLogicExecutionResults<>(ImmutableMap.of());

  public OutputLogicExecutionResults<T> withResults(
      Map<? extends FacetValues, CompletableFuture<@Nullable T>> results) {
    return new OutputLogicExecutionResults<>(ImmutableMap.copyOf(results));
  }

  @SuppressWarnings("unchecked")
  public static <T> OutputLogicExecutionResults<T> empty() {
    return (OutputLogicExecutionResults<T>) EMPTY;
  }
}

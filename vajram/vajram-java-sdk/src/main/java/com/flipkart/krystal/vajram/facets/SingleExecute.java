package com.flipkart.krystal.vajram.facets;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;

public record SingleExecute<T>(@Nullable T input, boolean shouldSkip, String doc)
    implements DependencyCommand<T> {

  @Override
  public ImmutableList<T> inputs() {
    if (input == null) {
      return ImmutableList.of();
    } else {
      return ImmutableList.of(input);
    }
  }

  public static <T> SingleExecute<T> executeWith(@Nullable T input) {
    return new SingleExecute<>(input, false, EMPTY_STRING);
  }

  public static <T> SingleExecute<T> skipExecution(String reason) {
    return new SingleExecute<>(null, true, reason);
  }
}

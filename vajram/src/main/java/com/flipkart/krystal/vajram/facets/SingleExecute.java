package com.flipkart.krystal.vajram.facets;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public record SingleExecute<T>(@Nullable T input, boolean shouldSkip, String doc)
    implements DependencyCommand<T> {

  @Override
  public ImmutableCollection<Optional<T>> inputs() {
    return ImmutableList.of(Optional.ofNullable(input));
  }

  public static <T> SingleExecute<T> executeWith(@Nullable T value) {
    return new SingleExecute<>(value, false, EMPTY_STRING);
  }

  public static <T> SingleExecute<T> skipExecution(String reason) {
    return new SingleExecute<>(null, true, reason);
  }
}

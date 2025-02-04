package com.flipkart.krystal.vajram.facets;

import com.google.common.collect.ImmutableList;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record SingleExecute<T>(
    @Nullable T input, boolean shouldSkip, String doc, @Nullable Throwable skipCause)
    implements DependencyCommand<T> {

  @Override
  public ImmutableList<@NonNull T> inputs() {
    if (input == null) {
      return ImmutableList.of();
    } else {
      return ImmutableList.of(input);
    }
  }

  public void ifPresent(Consumer<T> action) {
    if (input != null) {
      action.accept(input);
    }
  }

  public static <T> SingleExecute<T> executeWith(@Nullable T input) {
    return new SingleExecute<>(input, false, EMPTY_STRING, null);
  }

  public static <T> SingleExecute<T> skipExecution(String reason) {
    return skipExecution(reason, null);
  }

  public static <T> SingleExecute<T> skipExecution(String reason, @Nullable Throwable skipCause) {
    return new SingleExecute<>(null, true, reason, skipCause);
  }
}

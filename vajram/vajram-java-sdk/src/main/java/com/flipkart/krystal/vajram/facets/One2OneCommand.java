package com.flipkart.krystal.vajram.facets;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public record One2OneCommand<T>(
    @Nullable T input, boolean shouldSkip, String doc, @Nullable Throwable skipCause)
    implements DependencyCommand<T> {

  @SuppressWarnings("RedundantTypeArguments")
  @Override
  public List<@Nullable T> inputs() {
    return Collections.<@Nullable T>singletonList(input);
  }

  public void ifPresent(Consumer<@Nullable T> action) {
    action.accept(input);
  }

  public static <T> One2OneCommand<T> executeWith(@Nullable T input) {
    return new One2OneCommand<>(input, false, EMPTY_STRING, null);
  }

  public static <T> One2OneCommand<T> skipExecution(String reason) {
    return skipExecution(reason, null);
  }

  public static <T> One2OneCommand<T> skipExecution(String reason, @Nullable Throwable skipCause) {
    return new One2OneCommand<>(null, true, reason, skipCause);
  }
}

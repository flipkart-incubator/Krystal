package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.data.Errable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public record One2OneCommand<T>(
    Errable<T> input, boolean shouldSkip, String doc, @Nullable Throwable skipCause)
    implements DependencyCommand<T> {

  @SuppressWarnings("RedundantTypeArguments")
  @Override
  public Collection<Errable<? extends T>> errables() {
    return List.of(input);
  }

  public void ifPresent(Consumer<T> action) {
    input.handle(_err -> {}, action);
  }

  public static <T> One2OneCommand<T> executeWith(@Nullable T input) {
    return new One2OneCommand<>(Errable.withValue(input), false, EMPTY_STRING, null);
  }

  public static <T> One2OneCommand<T> skipExecution(String reason) {
    return skipExecution(reason, null);
  }

  public static <T> One2OneCommand<T> skipExecution(String reason, @Nullable Throwable skipCause) {
    return new One2OneCommand<>(null, true, reason, skipCause);
  }
}

package com.flipkart.krystal.vajram;

import java.util.Optional;
import java.util.concurrent.Callable;

public record ValueOrError<T>(Optional<T> value, Optional<Throwable> error) {

  public ValueOrError {
    if ((value.isPresent() && error.isPresent())) {
      throw new IllegalArgumentException(
          "Both of 'value' and 'failureReason' cannot be present together");
    }
  }

  public ValueOrError(T value) {
    //noinspection rawtypes,unchecked
    this((value instanceof Optional o) ? o : Optional.ofNullable(value), Optional.empty());
  }

  public static <T> ValueOrError<T> from(Callable<T> valueProvider) {
    try {
      return new ValueOrError<>(valueProvider.call());
    } catch (Exception e) {
      return new ValueOrError<>(Optional.empty(), Optional.of(e));
    }
  }

  public static <T> ValueOrError<T> empty() {
    return new ValueOrError<>(Optional.empty(), Optional.empty());
  }
}

package com.flipkart.krystal.vajram.inputs;

import java.util.Optional;

public record ValueOrError<T>(Optional<T> value, Optional<Throwable> error) {

  public ValueOrError {
    if ((value.isPresent() && error.isPresent())) {
      throw new IllegalArgumentException(
          "Both of 'value' and 'failureReason' cannot be present together");
    }
  }

  public ValueOrError(T value) {
    //noinspection rawtypes
    this((value instanceof Optional o) ? o : Optional.ofNullable(value), Optional.empty());
  }

  public static <T> ValueOrError<T> empty() {
    return new ValueOrError<>(Optional.empty(), Optional.empty());
  }
}

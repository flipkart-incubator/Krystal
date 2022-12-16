package com.flipkart.krystal.vajram.inputs;

import java.util.Optional;

public record SingleValue<T>(Optional<T> value, Optional<Throwable> failureReason) {

  public SingleValue {
    if ((value.isPresent() && failureReason.isPresent())) {
      throw new IllegalArgumentException(
          "Both of 'value' and 'failureReason' cannot be present together");
    }
  }

  public SingleValue(T value) {
    this(Optional.of(value), Optional.empty());
  }

  public static <T> SingleValue<T> empty() {
    return new SingleValue<>(Optional.empty(), Optional.empty());
  }
}

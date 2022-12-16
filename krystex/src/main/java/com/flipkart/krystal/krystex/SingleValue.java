package com.flipkart.krystal.krystex;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record SingleValue<T>(Optional<T> value, Optional<Throwable> failureReason)
    implements Value {

  public SingleValue {
    if ((value.isPresent() && failureReason.isPresent())) {
      throw new IllegalArgumentException(
          "Both of 'value' and 'failureReason' cannot be present together");
    }
  }

  public SingleValue(T value, Throwable exception) {
    this(Optional.ofNullable(value), Optional.ofNullable(exception));
  }

  public SingleValue(T value) {
    this(Optional.of(value), Optional.empty());
  }

  public static <T> SingleValue<T> empty() {
    return new SingleValue<>(Optional.empty(), Optional.empty());
  }

  @Override
  public CompletableFuture<T> toFuture() {
    return isSuccessful()
        ? completedFuture(value().orElse(null))
        : failedFuture(failureReason().orElseThrow(IllegalStateException::new));
  }
}

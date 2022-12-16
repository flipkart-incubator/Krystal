package com.flipkart.krystal.krystex;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record MultiValue<T>(ImmutableList<T> values, Optional<Throwable> failureReason)
    implements Value {

  public MultiValue {
    if (!values.isEmpty() && failureReason.isPresent()) {
      throw new IllegalArgumentException(
          "Both of 'values' and 'failureReason' cannot be present together");
    }
  }

  @Override
  public CompletableFuture<ImmutableList<T>> toFuture() {
    return isSuccessful()
        ? completedFuture(values())
        : failedFuture(failureReason().orElseThrow(IllegalStateException::new));
  }
}

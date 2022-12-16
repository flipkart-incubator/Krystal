package com.flipkart.krystal.krystex;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Value {

  default boolean isFailure() {
    return !isSuccessful();
  }

  default boolean isSuccessful() {
    return failureReason().isEmpty();
  }

  CompletableFuture<?> toFuture();

  Optional<Throwable> failureReason();
}

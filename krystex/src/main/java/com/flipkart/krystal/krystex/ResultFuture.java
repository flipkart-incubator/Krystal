package com.flipkart.krystal.krystex;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public sealed interface ResultFuture permits SingleResultFuture, MultiResultFuture {

  CompletableFuture<?> future();

  default boolean isFailure() {
    return future().isCompletedExceptionally();
  }

  default boolean isSuccessful() {
    return future().isDone() && !future().isCompletedExceptionally();
  }

  default Optional<Throwable> getFailureReason() {
    if (future().isCompletedExceptionally()) {
      return Optional.ofNullable(future().handle((unused, throwable) -> throwable).getNow(null));
    } else {
      return Optional.empty();
    }
  }
}

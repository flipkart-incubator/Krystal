package com.flipkart.krystal.krystex;

import java.util.concurrent.CompletableFuture;

public sealed interface Result permits SingleResult, BatchResult {

  CompletableFuture<?> future();

  default boolean isFailure() {
    return future().isCompletedExceptionally();
  }

  default boolean isSuccessful() {
    return future().isDone() && !future().isCompletedExceptionally();
  }
}


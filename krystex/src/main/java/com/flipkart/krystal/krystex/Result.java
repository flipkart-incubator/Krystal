package com.flipkart.krystal.krystex;

import java.util.concurrent.CompletableFuture;

public record Result<T>(CompletableFuture<T> future) {

  public boolean isSuccess() {
    return !isFailure();
  }

  public boolean isFailure() {
    return future.isCompletedExceptionally();
  }
}

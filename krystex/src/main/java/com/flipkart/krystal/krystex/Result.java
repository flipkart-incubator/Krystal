package com.flipkart.krystal.krystex;

import java.util.concurrent.CompletableFuture;

public record Result<T>(CompletableFuture<T> future) {

  public Result() {
    this(new CompletableFuture<>());
  }

  public boolean isFailure() {
    return future.isCompletedExceptionally();
  }
}

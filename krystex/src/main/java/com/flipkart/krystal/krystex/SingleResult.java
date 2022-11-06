package com.flipkart.krystal.krystex;

import java.util.concurrent.CompletableFuture;

public record SingleResult<T>(CompletableFuture<T> future) implements Result {

  public SingleResult() {
    this(new CompletableFuture<>());
  }
}

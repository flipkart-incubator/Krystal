package com.flipkart.krystal.krystex;

import java.util.concurrent.CompletableFuture;

public record SingleResultFuture<T>(CompletableFuture<T> future) implements ResultFuture {

  public SingleResultFuture() {
    this(new CompletableFuture<>());
  }
}

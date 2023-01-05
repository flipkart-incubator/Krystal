package com.flipkart.krystal.krystex.node;

import java.util.concurrent.CompletableFuture;

public record NodeResponseFuture(
    CompletableFuture<NodeInputs> inputsFuture, CompletableFuture<Object> responseFuture) {

  public NodeResponseFuture() {
    this(new CompletableFuture<>(), new CompletableFuture<>());
  }
}

package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import java.util.concurrent.CompletableFuture;

public record NodeResponseFuture(
    CompletableFuture<Inputs> inputsFuture, CompletableFuture<Object> responseFuture) {

  public NodeResponseFuture() {
    this(new CompletableFuture<>(), new CompletableFuture<>());
  }
}

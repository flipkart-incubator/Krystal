package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.ValueOrError;
import java.util.concurrent.CompletableFuture;

public record NodeResponseFuture(CompletableFuture<ValueOrError<Object>> responseFuture) {

  public NodeResponseFuture() {
    this(new CompletableFuture<>());
  }
}

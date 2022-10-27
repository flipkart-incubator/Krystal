package com.flipkart.krystal.krystex;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class NonBlockingNodeDefinition<T> extends NodeDefinition<T> {

  public NonBlockingNodeDefinition(String nodeId, Set<String> inputs) {
    super(nodeId, inputs);
  }

  @Override
  public final CompletableFuture<T> logic() {
    return completedFuture(nonBlockingLogic());
  }

  protected abstract T nonBlockingLogic();
}

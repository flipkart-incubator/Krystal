package com.flipkart.krystal.krystex;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class NonBlockingNodeDefinition<T> extends NodeDefinition<T> {

  NonBlockingNodeDefinition(String nodeId, Set<String> inputs) {
    super(nodeId, inputs);
  }

  @Override
  public final CompletableFuture<T> logic(ImmutableMap<String, ?> dependencyValues) {
    return completedFuture(nonBlockingLogic(dependencyValues));
  }

  protected abstract T nonBlockingLogic(ImmutableMap<String, ?> dependencyValues);
}

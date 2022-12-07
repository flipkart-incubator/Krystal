package com.flipkart.krystal.krystex;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class BlockingNodeDefinition<T> extends NodeDefinition<T> {

  BlockingNodeDefinition(String nodeId, Set<String> dependencies, Map<String, String> dependencyProviders, Set<String> inputs) {
    super(nodeId, dependencies, dependencyProviders, inputs);
  }

  @Override
  public final CompletableFuture<ImmutableList<T>> logic(ImmutableMap<String, ?> dependencyValues) {
    return blockingLogic(dependencyValues);
  }

  protected abstract CompletableFuture<ImmutableList<T>> blockingLogic(ImmutableMap<String, ?> dependencyValues);

}

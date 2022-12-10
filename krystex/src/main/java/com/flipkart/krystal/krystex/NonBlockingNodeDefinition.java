package com.flipkart.krystal.krystex;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class NonBlockingNodeDefinition<T> extends NodeDefinition<T> {

  NonBlockingNodeDefinition(
      String nodeId, Set<String> inputNames, Map<String, String> inputProviders) {
    super(nodeId, inputNames, inputProviders, ImmutableMap.of());
  }

  @Override
  public final CompletableFuture<ImmutableList<T>> logic(NodeInputs dependencyValues) {
    return completedFuture(nonBlockingLogic(dependencyValues));
  }

  protected abstract ImmutableList<T> nonBlockingLogic(NodeInputs dependencyValues);
}

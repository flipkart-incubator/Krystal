package com.flipkart.krystal.vajram;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class ComputeVajram<T> extends AbstractVajram<T> {

  @Override
  public final ImmutableMap<Inputs, CompletableFuture<T>> execute(
      ImmutableList<Inputs> inputsList) {
    return executeCompute(inputsList).entrySet().stream()
        .collect(toImmutableMap(Entry::getKey, e -> completedFuture(e.getValue())));
  }

  public abstract ImmutableMap<Inputs, T> executeCompute(
      ImmutableList<Inputs> inputsList);
}

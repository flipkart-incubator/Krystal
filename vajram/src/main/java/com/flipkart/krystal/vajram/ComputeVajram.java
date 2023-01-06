package com.flipkart.krystal.vajram;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.inputs.InputValues;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class ComputeVajram<T> extends AbstractVajram<T> {

  public ImmutableMap<InputValues, T> executeCompute(ImmutableList<InputValues> inputsList) {
    throw new UnsupportedOperationException(
        "executeCompute method should be implemented by a ComputeVajram");
  }

  @Override
  public final ImmutableMap<InputValues, CompletableFuture<T>> execute(
      ImmutableList<InputValues> inputsList) {
    return executeCompute(inputsList).entrySet().stream()
        .collect(toImmutableMap(Entry::getKey, e -> completedFuture(e.getValue())));
  }
}

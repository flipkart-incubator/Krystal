package com.flipkart.krystal.vajram;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract non-sealed class ComputeVajram<T> extends AbstractVajram<T> {

  @Override
  public final ImmutableMap<Inputs, CompletableFuture<@Nullable T>> execute(
      ImmutableList<Inputs> inputsList) {
    try {
      return executeCompute(inputsList).entrySet().stream()
          .collect(toImmutableMap(Entry::getKey, ComputeVajram::toFuture));
    } catch (Throwable e) {
      return inputsList.stream().collect(toImmutableMap(identity(), i -> failedFuture(e)));
    }
  }

  private static <T> CompletableFuture<@Nullable T> toFuture(Entry<Inputs, ValueOrError<T>> e) {
    return e.getValue().toFuture();
  }

  public abstract ImmutableMap<Inputs, ValueOrError<T>> executeCompute(
      ImmutableList<Inputs> inputsList);
}

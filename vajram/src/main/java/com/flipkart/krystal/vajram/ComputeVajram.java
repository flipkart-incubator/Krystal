package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.data.ValueOrError.valueOrError;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class ComputeVajram<T> extends AbstractVajram<T> {

  @Override
  public final ImmutableMap<Inputs, CompletableFuture<T>> execute(
      ImmutableList<Inputs> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                identity(), inputs -> valueOrError(() -> executeCompute(inputs)).toFuture()));
  }

  public abstract T executeCompute(Inputs inputs) throws Exception;
}

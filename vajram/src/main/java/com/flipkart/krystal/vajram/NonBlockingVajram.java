package com.flipkart.krystal.vajram;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class NonBlockingVajram<T> extends AbstractVajram<T> {

  @Override
  public final boolean isBlockingVajram() {
    return false;
  }

  @Override
  public final CompletableFuture<ImmutableList<T>> execute(ExecutionContext executionContext) {
    return completedFuture(executeNonBlocking(executionContext));
  }

  public abstract ImmutableList<T> executeNonBlocking(ExecutionContext executionContext);

}

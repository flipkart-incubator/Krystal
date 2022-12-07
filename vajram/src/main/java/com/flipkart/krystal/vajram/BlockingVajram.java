package com.flipkart.krystal.vajram;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.CompletableFuture;

public abstract sealed class BlockingVajram<T> extends AbstractVajram<T>
    permits DefaultModulatedBlockingVajram, UnmodulatedAsyncVajram {

  @Override
  public final boolean isBlockingVajram() {
    return true;
  }

  @Override
  public final CompletableFuture<ImmutableList<T>> execute(ExecutionContext executionContext) {
    return executeBlocking(executionContext);
  }

  public abstract CompletableFuture<ImmutableList<T>> executeBlocking(ExecutionContext executionContext);
}

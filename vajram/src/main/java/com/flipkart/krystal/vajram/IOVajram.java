package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class IOVajram<T> extends AbstractVajram<T> {

  @Override
  public final boolean isIOVajram() {
    return true;
  }

  public abstract ImmutableMap<?, CompletableFuture<T>> execute(
      ModulatedExecutionContext executionContext);

  @Override
  public ImmutableList<RequestBuilder<?>> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    return ImmutableList.of();
  }

  public abstract InputsConverter<?, ?, ?, ?> getInputsConvertor();
}

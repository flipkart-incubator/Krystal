package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class IOVajram<T> extends AbstractVajram<T> {

  @Override
  public final boolean isIOVajram() {
    return true;
  }

  public ImmutableMap<?, CompletableFuture<T>> execute(ModulatedExecutionContext executionContext) {
    throw new UnsupportedOperationException("execute method should be implemented by an IOVajram");
  }

  public InputsConverter<?, ?, ?> getInputsConvertor() {
    throw new UnsupportedOperationException(
        "getInputsConvertor method should be implemented by an IOVajram");
  }
}

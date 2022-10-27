package com.flipkart.krystal.vajram.modulation;

import static java.util.Collections.emptyList;

import com.flipkart.krystal.vajram.inputs.InputModulator;
import com.google.common.collect.ImmutableList;

public class NoopInputModulator<Request> implements InputModulator<Request, Void, Request> {

  private static final NoopInputModulator<?> INSTANCE = new NoopInputModulator<>();

  @Override
  public ImmutableList<ModulatedInput<Void, Request>> add(Request unModulatedRequest) {
    return ImmutableList.of(new ModulatedInput<>(emptyList(), unModulatedRequest));
  }

  @Override
  public ImmutableList<ModulatedInput<Void, Request>> modulate() {
    return ImmutableList.of();
  }

  public static <T> NoopInputModulator<T> noopInputModulator() {
    //noinspection unchecked
    return (NoopInputModulator<T>) INSTANCE;
  }

  private NoopInputModulator() {}
}

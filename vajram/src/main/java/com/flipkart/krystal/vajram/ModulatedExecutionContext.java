package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;

public class ModulatedExecutionContext implements ExecutionContext {
  private final ModulatedInput<?, ?> modulatedInput;

  public ModulatedExecutionContext(ModulatedInput<?, ?> modulatedInput) {
    this.modulatedInput = modulatedInput;
  }

  public <I, C> ModulatedInput<I, C> getModulatedInput() {
    //noinspection unchecked
    return (ModulatedInput<I, C>) modulatedInput;
  }
}

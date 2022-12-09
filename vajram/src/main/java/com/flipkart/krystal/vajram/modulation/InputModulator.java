package com.flipkart.krystal.vajram.modulation;

import com.google.common.collect.ImmutableList;
import java.util.function.Consumer;

public interface InputModulator<Request, InputsNeedingModulation, CommonInputs> {

  ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>> add(Request request);

  ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>> terminate();

  /**
   * When this InputModulator decides to terminate (due to some internal state like a timer), or
   * when the {@link #terminate()} method is called, execute the given callback.
   */
  void onTermination(Consumer<ModulatedInput<InputsNeedingModulation, CommonInputs>> callback);

  interface ModulatedInput<InputsNeedingModulation, CommonInputs> {
    ImmutableList<InputsNeedingModulation> inputsNeedingModulation();

    CommonInputs commonInputs();
  }
}

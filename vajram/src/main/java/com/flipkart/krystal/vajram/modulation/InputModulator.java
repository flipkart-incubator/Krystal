package com.flipkart.krystal.vajram.modulation;

import com.google.common.collect.ImmutableList;
import java.util.function.Consumer;

public interface InputModulator<InputsNeedingModulation, CommonInputs> {

  ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>> add(
      InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs);

  ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>> terminate();

  /**
   * When this InputModulator decides to terminate (due to some internal state like a timer), or
   * when the {@link #terminate()} method is called, execute the given callback.
   */
  void onInternalTermination(
      Consumer<ModulatedInput<InputsNeedingModulation, CommonInputs>> callback);

  record ModulatedInput<InputsNeedingModulation, CommonInputs>(
      ImmutableList<InputsNeedingModulation> inputsNeedingModulation, CommonInputs commonInputs) {}
}

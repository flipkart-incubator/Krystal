package com.flipkart.krystal.vajram.inputs;

import com.google.common.collect.ImmutableList;
import java.util.List;

public interface InputModulator<Request, InputsNeedingModulation, CommonInputs> {

  ImmutableList<ModulatedInput<InputsNeedingModulation,CommonInputs>> add(Request request);

  ImmutableList<ModulatedInput<InputsNeedingModulation,CommonInputs>> modulate();

  record ModulatedInput<InputsNeedingModulation,CommonInputs>(
      List<InputsNeedingModulation> inputsNeedingModulation, CommonInputs commonInputs){ }
}

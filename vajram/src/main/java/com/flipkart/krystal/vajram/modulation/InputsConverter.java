package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.vajram.inputs.InputValues;

public interface InputsConverter<AllInputs, InputsNeedingModulation, CommonInputs> {
  InputValues toMap(AllInputs allInputs);

  AllInputs enrichedRequest(InputValues inputValues);

  AllInputs enrichedRequest(
      InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs);

  CommonInputs commonInputs(AllInputs request);

  InputsNeedingModulation inputsNeedingModulation(AllInputs request);
}

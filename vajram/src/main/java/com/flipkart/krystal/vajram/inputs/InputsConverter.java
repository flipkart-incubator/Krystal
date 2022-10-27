package com.flipkart.krystal.vajram.inputs;

public interface InputsConverter<Request, InputsNeedingModulation, CommonInputs> {
  Request createRequestFrom(
      InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs);

  InputsNeedingModulation extractInputsNeedingModulation(Request request);

  CommonInputs extractCommonInputs(Request request);
}

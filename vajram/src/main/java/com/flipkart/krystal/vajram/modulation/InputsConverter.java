package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.vajram.inputs.InputValues;

public interface InputsConverter<EnrichedRequest, InputsNeedingModulation, CommonInputs> {
  InputValues toMap(EnrichedRequest enrichedRequest);

  EnrichedRequest enrichedRequest(InputValues inputValues);

  EnrichedRequest enrichedRequest(
      InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs);

  CommonInputs commonInputs(EnrichedRequest request);

  InputsNeedingModulation inputsNeedingModulation(EnrichedRequest request);
}

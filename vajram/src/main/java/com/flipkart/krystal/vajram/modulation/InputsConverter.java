package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.google.common.collect.ImmutableMap;
import java.util.List;

public interface InputsConverter<
    EnrichedRequest,
    InputsNeedingModulation,
    CommonInputs,
    ModulatedRequest extends ModulatedInput<InputsNeedingModulation, CommonInputs>> {
  InputValues toMap(EnrichedRequest enrichedRequest);

  EnrichedRequest enrichedRequest(InputValues inputValues);

  EnrichedRequest enrichedRequest(
      InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs);

  ModulatedRequest toModulatedRequest(List<EnrichedRequest> enrichedRequests);
}

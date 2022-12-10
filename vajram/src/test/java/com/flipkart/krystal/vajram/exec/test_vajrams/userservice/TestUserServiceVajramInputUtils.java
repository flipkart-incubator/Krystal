package com.flipkart.krystal.vajram.exec.test_vajrams.userservice;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;

class TestUserServiceVajramInputUtils {
  record InputsNeedingModulation(String userId) {}

  record CommonInputs() {}

  record EnrichedRequest(InputsNeedingModulation request, CommonInputs common) {}

  record ModulatedRequest(
      ImmutableList<InputsNeedingModulation> inputsNeedingModulation, CommonInputs commonInputs)
      implements ModulatedInput<InputsNeedingModulation, CommonInputs> {}

  static final InputsConverter<
          EnrichedRequest, InputsNeedingModulation, CommonInputs, ModulatedRequest>
      CONVERTER =
          new InputsConverter<>() {

            @Override
            public EnrichedRequest enrichedRequest(InputValues inputs) {
              return new EnrichedRequest(
                  new InputsNeedingModulation((String) inputs.values().get("user_id")),
                  new CommonInputs());
            }

            @Override
            public InputValues toMap(EnrichedRequest enrichedRequest) {
              return new InputValues(
                  ImmutableMap.<String, Object>builder()
                      .put("user_id", enrichedRequest.request().userId())
                      .build());
            }

            @Override
            public EnrichedRequest enrichedRequest(
                InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs) {
              return new EnrichedRequest(inputsNeedingModulation, commonInputs);
            }

            @Override
            public ModulatedRequest toModulatedRequest(List<EnrichedRequest> enrichedRequests) {
              if (enrichedRequests.isEmpty()) {
                throw new IllegalArgumentException(
                    "Cannot create modulated request from zero requests");
              }
              return new ModulatedRequest(
                  enrichedRequests.stream()
                      .map(EnrichedRequest::request)
                      .collect(toImmutableList()),
                  enrichedRequests.iterator().next().common());
            }
          };
}

package com.flipkart.krystal.vajram.exec.test_vajrams.userservice;

import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableMap;

class TestUserServiceInputUtils {
  record InputsNeedingModulation(String userId) {}

  record CommonInputs() {}

  record EnrichedRequest(InputsNeedingModulation request, CommonInputs common) {}

  static final InputsConverter<EnrichedRequest, InputsNeedingModulation, CommonInputs> CONVERTER =
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
        public CommonInputs commonInputs(EnrichedRequest enrichedRequest) {
          return new CommonInputs();
        }

        @Override
        public InputsNeedingModulation inputsNeedingModulation(EnrichedRequest enrichedRequest) {
          return enrichedRequest.request();
        }
      };
}

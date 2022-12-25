package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice;

import static com.flipkart.krystal.vajram.inputs.SingleValue.empty;

import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.SingleValue;
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
              new InputsNeedingModulation(
                  (String) inputs.values().getOrDefault("user_id", empty()).value().orElseThrow()),
              new CommonInputs());
        }

        @Override
        public InputValues toMap(EnrichedRequest enrichedRequest) {
          return new InputValues(
              ImmutableMap.<String, SingleValue<?>>builder()
                  .put("user_id", new SingleValue<>(enrichedRequest.request().userId()))
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

package com.flipkart.krystal.vajram.exec.test_vajrams.friendsservice;

import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.SingleValue;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableMap;

class FriendsServiceInputUtils {
  record InputsNeedingModulation(String userId) {}

  record CommonInputs() {}

  record EnrichedRequest(InputsNeedingModulation inputsNeedingModulation, CommonInputs common) {}

  static final InputsConverter<EnrichedRequest, InputsNeedingModulation, CommonInputs> CONVERTER =
      new InputsConverter<>() {
        @Override
        public InputValues toMap(EnrichedRequest enrichedRequest) {
          return new InputValues(
              ImmutableMap.of(
                  "user_id",
                  new SingleValue<>(enrichedRequest.inputsNeedingModulation().userId())));
        }

        @Override
        public EnrichedRequest enrichedRequest(InputValues inputs) {
          return new EnrichedRequest(
              new InputsNeedingModulation(inputs.<String>getValue("user_id").value().orElseThrow()),
              new CommonInputs());
        }

        @Override
        public EnrichedRequest enrichedRequest(
            InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs) {
          return new EnrichedRequest(inputsNeedingModulation, commonInputs);
        }

        @Override
        public CommonInputs commonInputs(EnrichedRequest enrichedRequest) {
          return enrichedRequest.common();
        }

        @Override
        public InputsNeedingModulation inputsNeedingModulation(EnrichedRequest enrichedRequest) {
          return enrichedRequest.inputsNeedingModulation();
        }
      };
}

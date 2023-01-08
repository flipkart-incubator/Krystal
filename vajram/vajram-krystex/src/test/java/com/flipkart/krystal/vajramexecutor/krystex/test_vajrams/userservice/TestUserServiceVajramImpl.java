package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceInputUtil.CONVERTER;

import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.data.InputValues;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceInputUtil.CommonInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceInputUtil.InputsNeedingModulation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TestUserServiceVajramImpl extends TestUserServiceVajram {
  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder()
            // Local name for this input
            .name(USER_ID)
            // Data type - used for code generation
            .type(string())
            // If this input is not provided by the client, throw a build time error.
            .isMandatory()
            .needsModulation()
            .build());
  }

  @Override
  public ImmutableMap<InputValues, CompletableFuture<TestUserInfo>> execute(
      ImmutableList<InputValues> inputsList) {
    Map<InputsNeedingModulation, InputValues> mapping = new HashMap<>();
    List<InputsNeedingModulation> ims = new ArrayList<>();
    CommonInputs commonInputs = null;
    for (InputValues inputs : inputsList) {
      UnmodulatedInput<InputsNeedingModulation, CommonInputs> allInputs =
          getInputsConvertor().apply(inputs);
      commonInputs = allInputs.commonInputs();
      InputsNeedingModulation im = allInputs.inputsNeedingModulation();
      mapping.put(im, inputs);
      ims.add(im);
    }
    Map<InputValues, CompletableFuture<TestUserInfo>> returnValue = new LinkedHashMap<>();

    if (commonInputs != null) {
      ImmutableMap<InputsNeedingModulation, CompletableFuture<TestUserInfo>> results =
          callUserService(new ModulatedInput<>(ImmutableList.copyOf(ims), commonInputs));
      results.forEach((im, future) -> returnValue.put(mapping.get(im), future));
    }
    return ImmutableMap.copyOf(returnValue);
  }

  @Override
  public InputsConverter<InputsNeedingModulation, CommonInputs> getInputsConvertor() {
    return CONVERTER;
  }
}

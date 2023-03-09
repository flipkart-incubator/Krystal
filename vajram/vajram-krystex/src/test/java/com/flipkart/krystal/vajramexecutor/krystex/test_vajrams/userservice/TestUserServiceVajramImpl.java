package com.flipkart.krystal.vajramexecutor.krystex.testVajrams.userservice;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajramexecutor.krystex.testVajrams.userservice.TestUserServiceInputUtil.CONVERTER;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.flipkart.krystal.vajramexecutor.krystex.testVajrams.userservice.TestUserServiceInputUtil.TestUserServiceCommonInputs;
import com.flipkart.krystal.vajramexecutor.krystex.testVajrams.userservice.TestUserServiceInputUtil.TestUserServiceInputsNeedingModulation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class TestUserServiceVajramImpl extends TestUserServiceVajram {
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
  public ImmutableMap<Inputs, CompletableFuture<TestUserInfo>> execute(
      ImmutableList<Inputs> inputsList) {
    Map<TestUserServiceInputsNeedingModulation, Inputs> mapping = new HashMap<>();
    List<TestUserServiceInputsNeedingModulation> ims = new ArrayList<>();
    TestUserServiceCommonInputs commonInputs = null;
    for (Inputs inputs : inputsList) {
      UnmodulatedInput<TestUserServiceInputsNeedingModulation, TestUserServiceCommonInputs>
          allInputs = getInputsConvertor().apply(inputs);
      commonInputs = allInputs.commonInputs();
      TestUserServiceInputsNeedingModulation im = allInputs.inputsNeedingModulation();
      mapping.put(im, inputs);
      ims.add(im);
    }
    Map<Inputs, CompletableFuture<TestUserInfo>> returnValue = new LinkedHashMap<>();

    if (commonInputs != null) {
      ImmutableMap<TestUserServiceInputsNeedingModulation, CompletableFuture<TestUserInfo>>
          results = callUserService(new ModulatedInput<>(ImmutableList.copyOf(ims), commonInputs));
      results.forEach((im, future) -> returnValue.put(mapping.get(im), future));
    }
    return ImmutableMap.copyOf(returnValue);
  }

  @Override
  public InputsConverter<TestUserServiceInputsNeedingModulation, TestUserServiceCommonInputs>
      getInputsConvertor() {
    return CONVERTER;
  }
}

package com.flipkart.krystal.vajram.samples.greeting;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.samples.greeting.UserServiceInputUtil.CONVERTER;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceInputUtil.UserServiceCommonInputs;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceInputUtil.UserServiceInputsNeedingModulation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class UserServiceVajramImpl extends UserServiceVajram {
  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder()
            // Local name for this input
            .name("user_id")
            // Data type - used for code generation
            .type(string())
            // If this input is not provided by the client, throw a build time error.
            .isMandatory()
            .needsModulation()
            .build());
  }

  @Override
  public InputsConverter<UserServiceInputsNeedingModulation, UserServiceCommonInputs> getInputsConvertor() {
    return CONVERTER;
  }

  @Override
  public ImmutableMap<Inputs, CompletableFuture<UserInfo>> execute(
      ImmutableList<Inputs> inputsList) {
    Map<UserServiceInputsNeedingModulation, Inputs> mapping = new HashMap<>();
    List<UserServiceInputsNeedingModulation> ims = new ArrayList<>();
      UserServiceCommonInputs commonInputs = null;
    for (Inputs inputs : inputsList) {
      UnmodulatedInput<UserServiceInputsNeedingModulation, UserServiceCommonInputs> allInputs =
          getInputsConvertor().apply(inputs);
      commonInputs = allInputs.commonInputs();
        UserServiceInputsNeedingModulation im = allInputs.inputsNeedingModulation();
      mapping.put(im, inputs);
      ims.add(im);
    }
    Map<Inputs, CompletableFuture<UserInfo>> returnValue = new LinkedHashMap<>();

    if (commonInputs != null) {
      var results = callUserService(new ModulatedInput<>(ImmutableList.copyOf(ims), commonInputs));
      results.forEach((im, future) -> returnValue.put(mapping.get(im), future));
    }
    return ImmutableMap.copyOf(returnValue);
  }
}

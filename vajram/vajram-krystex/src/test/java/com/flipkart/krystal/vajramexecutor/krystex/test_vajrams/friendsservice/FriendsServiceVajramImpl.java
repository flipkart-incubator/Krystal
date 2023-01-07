package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceInputUtil.CONVERTER;

import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceInputUtil.CommonInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceInputUtil.InputsNeedingModulation;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FriendsServiceVajramImpl extends FriendsServiceVajram {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name(USER_ID).type(string()).isMandatory().needsModulation().build());
  }

  @Override
  public ImmutableMap<InputValues, CompletableFuture<Set<String>>> execute(
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
    Map<InputValues, CompletableFuture<Set<String>>> returnValue = new LinkedHashMap<>();

    if (commonInputs != null) {
      ImmutableMap<InputsNeedingModulation, CompletableFuture<Set<String>>> results =
          call(new ModulatedInput<>(ImmutableList.copyOf(ims), commonInputs));
      results.forEach((im, future) -> returnValue.put(mapping.get(im), future));
    }
    return ImmutableMap.copyOf(returnValue);
  }

  @Override
  public InputsConverter<InputsNeedingModulation, CommonInputs> getInputsConvertor() {
    return CONVERTER;
  }
}

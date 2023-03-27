package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceInputUtil.CONVERTER;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceInputUtil.FriendsServiceCommonInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceInputUtil.FriendsServiceInputsNeedingModulation;
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

public final class FriendsServiceVajramImpl123 {

  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name(FriendsServiceVajram.USER_ID).type(string()).isMandatory().needsModulation().build());
  }

  public ImmutableMap<Inputs, CompletableFuture<Set<String>>> execute(
      ImmutableList<Inputs> inputsList) {
    Map<FriendsServiceInputsNeedingModulation, Inputs> mapping = new HashMap<>();
    List<FriendsServiceInputsNeedingModulation> ims = new ArrayList<>();
    FriendsServiceCommonInputs commonInputs = null;
    for (Inputs inputs : inputsList) {
      UnmodulatedInput<FriendsServiceInputsNeedingModulation, FriendsServiceCommonInputs>
          allInputs = getInputsConvertor().apply(inputs);
      commonInputs = allInputs.commonInputs();
      FriendsServiceInputsNeedingModulation im = allInputs.inputsNeedingModulation();
      mapping.put(im, inputs);
      ims.add(im);
    }
    Map<Inputs, CompletableFuture<Set<String>>> returnValue = new LinkedHashMap<>();

    if (commonInputs != null) {
      ImmutableMap<FriendsServiceInputsNeedingModulation, CompletableFuture<Set<String>>> results =
          FriendsServiceVajram.call(new ModulatedInput<>(ImmutableList.copyOf(ims), commonInputs));
      results.forEach((im, future) -> returnValue.put(mapping.get(im), future));
    }
    return ImmutableMap.copyOf(returnValue);
  }

  public InputsConverter<FriendsServiceInputsNeedingModulation, FriendsServiceCommonInputs>
      getInputsConvertor() {
    return CONVERTER;
  }
}

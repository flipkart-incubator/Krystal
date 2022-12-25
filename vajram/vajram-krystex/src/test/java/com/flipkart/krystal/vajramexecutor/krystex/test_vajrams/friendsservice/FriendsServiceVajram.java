package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice;

import static com.flipkart.krystal.datatypes.StringType.string;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceInputUtils.CommonInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceInputUtils.EnrichedRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceInputUtils.InputsNeedingModulation;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@VajramDef(FriendsServiceVajram.ID)
public abstract class FriendsServiceVajram extends IOVajram<Set<String>> {

  public static final String ID = "FriendsServiceVajram";
  public static final String USER_ID = "user_id";

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name(USER_ID).type(string()).mandatory().needsModulation().build());
  }

  public ImmutableMap<EnrichedRequest, CompletableFuture<Set<String>>> call(
      ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput) {
    Map<EnrichedRequest, CompletableFuture<Set<String>>> result = new LinkedHashMap<>();
    for (InputsNeedingModulation inputsNeedingModulation :
        modulatedInput.inputsNeedingModulation()) {
      String userId = inputsNeedingModulation.userId();
      result.put(
          new EnrichedRequest(inputsNeedingModulation, modulatedInput.commonInputs()),
          completedFuture(getFriends(userId)));
    }
    return ImmutableMap.copyOf(result);
  }

  private Set<String> getFriends(String userId) {
    return ImmutableSet.of(userId + ":friend1", userId + ":friend2");
  }
}

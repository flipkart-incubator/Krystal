package com.flipkart.krystal.vajramexecutor.krystex.testVajrams.friendsservice;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajramexecutor.krystex.testVajrams.friendsservice.FriendsServiceInputUtil.FriendsServiceCommonInputs;
import com.flipkart.krystal.vajramexecutor.krystex.testVajrams.friendsservice.FriendsServiceInputUtil.FriendsServiceInputsNeedingModulation;
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

  @VajramLogic
  public ImmutableMap<FriendsServiceInputsNeedingModulation, CompletableFuture<Set<String>>> call(
      ModulatedInput<FriendsServiceInputsNeedingModulation, FriendsServiceCommonInputs> modulatedInput) {
    Map<FriendsServiceInputsNeedingModulation, CompletableFuture<Set<String>>> result = new LinkedHashMap<>();
    for (FriendsServiceInputsNeedingModulation inputsNeedingModulation :
        modulatedInput.inputsNeedingModulation()) {
      String userId = inputsNeedingModulation.userId();
      result.put(inputsNeedingModulation, completedFuture(getFriends(userId)));
    }
    return ImmutableMap.copyOf(result);
  }

  private Set<String> getFriends(String userId) {
    return ImmutableSet.of(userId + ":friend1", userId + ":friend2");
  }
}

package com.flipkart.krystal.vajram.samples.greeting;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceInputUtil.UserServiceCommonInputs;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceInputUtil.UserServiceInputsNeedingModulation;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
@VajramDef(UserServiceVajram.ID)
public abstract class UserServiceVajram extends IOVajram<UserInfo> {

  public static final String ID = "userServiceVajram";
  public static final String USER_ID = "user_id";

  @VajramLogic
  public ImmutableMap<UserServiceInputsNeedingModulation, CompletableFuture<UserInfo>> callUserService(
      ModulatedInput<UserServiceInputsNeedingModulation, UserServiceCommonInputs> modulatedRequest) {

    // Make a call to user service and get user info
    CompletableFuture<List<UserInfo>> serviceResponse =
        batchServiceCall(modulatedRequest.inputsNeedingModulation());

    CompletableFuture<Map<UserServiceInputsNeedingModulation, UserInfo>> resultsFuture =
        serviceResponse.thenApply(
            userInfos ->
                userInfos.stream()
                    .collect(
                        Collectors.toMap(
                            userInfo -> new UserServiceInputsNeedingModulation(userInfo.userId()),
                            userInfo -> userInfo )));
    return modulatedRequest.inputsNeedingModulation().stream()
        .collect(
            toImmutableMap(im -> im, im -> resultsFuture.thenApply(results -> results.get(im))));
  }

  private static CompletableFuture<List<UserInfo>> batchServiceCall(
      List<UserServiceInputsNeedingModulation> inputsNeedingModulations) {
    return completedFuture(
        inputsNeedingModulations.stream()
            .map(UserServiceInputsNeedingModulation::userId)
            .map(userId -> new UserInfo(userId, "Firstname Lastname (%s)".formatted(userId)))
            .toList());
  }
}

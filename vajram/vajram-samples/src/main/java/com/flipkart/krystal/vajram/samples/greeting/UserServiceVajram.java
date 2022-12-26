package com.flipkart.krystal.vajram.samples.greeting;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceVajramInputUtils.CommonInputs;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceVajramInputUtils.InputsNeedingModulation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class UserServiceVajram extends IOVajram<UserInfo> {

  public static final String ID = "userServiceVajram";
  public static final String USER_ID = "user_id";

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

  @VajramLogic
  public ImmutableMap<UserServiceVajramRequest, CompletableFuture<UserInfo>> callUserService(
      ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedRequest) {
    Set<String> userIds =
        modulatedRequest.inputsNeedingModulation().stream()
            .map(InputsNeedingModulation::userId)
            .collect(Collectors.toSet());

    // Make a call to user service and get user info
    return userIds.stream()
        .collect(
            toImmutableMap(
                s -> UserServiceVajramRequest.builder().userId(s).build(),
                userId ->
                    completedFuture(new UserInfo("Firstname Lastname (%s)".formatted(userId)))));
  }
}

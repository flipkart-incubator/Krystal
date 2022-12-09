package com.flipkart.krystal.vajram.exec.test_vajrams.userservice;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceVajram.ID;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceVajramInputUtils.EnrichedRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceVajramInputUtils.ModulatedRequest;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

@VajramDef(ID)
public abstract class TestUserServiceVajram extends IOVajram<TestUserInfo> {

  public static final String ID = "userServiceVajram";

  private static final Timer TIMER = new Timer();

  @Override
  public List<VajramInputDefinition> getInputDefinitions() {
    return List.of(
        Input.builder()
            // Local name for this input
            .name("user_id")
            // Data type - used for code generation
            .type(string())
            // If this input is not provided by the client, throw a build time error.
            .mandatory()
            .needsModulation()
            .build());
  }

  @VajramLogic
  public ImmutableMap<EnrichedRequest, CompletableFuture<TestUserInfo>> callUserService(
      ModulatedRequest modulatedRequest) {

    // Make a call to user service and get user info
    return modulatedRequest.inputsNeedingModulation().stream()
        .collect(
            toImmutableMap(
                inputsNeedingModulation ->
                    new EnrichedRequest(inputsNeedingModulation, modulatedRequest.commonInputs()),
                modInputs -> {
                  CompletableFuture<TestUserInfo> future = new CompletableFuture<>();
                  TIMER.schedule(
                      new TimerTask() {
                        @Override
                        public void run() {
                          future.complete(
                              new TestUserInfo(
                                  "Firstname Lastname (%s)".formatted(modInputs.userId())));
                        }
                      },
                      500);
                  return future;
                }));
  }
}

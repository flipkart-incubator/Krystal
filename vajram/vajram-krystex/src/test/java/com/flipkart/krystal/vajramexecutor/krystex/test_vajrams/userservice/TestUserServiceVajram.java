package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceInputUtils.CommonInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceInputUtils.EnrichedRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceInputUtils.InputsNeedingModulation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.LongAdder;

@VajramDef(TestUserServiceVajram.ID)
public class TestUserServiceVajram extends IOVajram<TestUserInfo> {

  public static final String ID = "testUserServiceVajram";
  public static final String USER_ID = "user_id";

  private static final ScheduledExecutorService LATENCY_INDUCER =
      newSingleThreadScheduledExecutor();

  public static final LongAdder CALL_COUNTER = new LongAdder();
  public static final Set<TestUserServiceRequest> REQUESTS = new LinkedHashSet<>();

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

  @VajramLogic
  public ImmutableMap<EnrichedRequest, CompletableFuture<TestUserInfo>> callUserService(
      ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedRequest) {
    CALL_COUNTER.increment();
    modulatedRequest.inputsNeedingModulation().stream()
        .map(im -> TestUserServiceRequest.builder().userId(im.userId()).build())
        .forEach(REQUESTS::add);

    // Make a call to user service and get user info
    return modulatedRequest.inputsNeedingModulation().stream()
        .collect(
            toImmutableMap(
                inputsNeedingModulation ->
                    new EnrichedRequest(inputsNeedingModulation, modulatedRequest.commonInputs()),
                modInputs -> {
                  CompletableFuture<TestUserInfo> future = new CompletableFuture<>();
                  LATENCY_INDUCER.schedule(
                      (Runnable)
                          () ->
                              future.complete(
                                  new TestUserInfo(
                                      "Firstname Lastname (%s)".formatted(modInputs.userId()))),
                      50,
                      MILLISECONDS);
                  return future;
                }));
  }
}

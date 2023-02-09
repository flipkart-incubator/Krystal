package com.flipkart.krystal.vajram.samples.greeting;

import static com.flipkart.krystal.data.ValueOrError.valueOrError;
import static com.flipkart.krystal.datatypes.JavaType.java;
import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.samples.greeting.GreetingInputUtil.AllInputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.lang.System.Logger;
import java.util.Map.Entry;
import java.util.Set;

// Auto-generated and managed by Krystal
public final class GreetingVajramImpl extends GreetingVajram {

  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder()
            // Local name for this input
            .name("user_id")
            // Data type - used for code generation
            .type(string())
            // If this input is not provided by the client, throw a build time error.
            .isMandatory()
            .build(),
        Dependency.builder()
            // Data type of resolved dependencies is inferred from the
            // dependency vajram's Definition
            .name("")
            // GreetingVajram needs UserService's Response to compose the Greeting
            // which it can get from the UserServiceVajram
            // (which is an Async Vajram as it makes network calls.
            .dataAccessSpec(vajramID(UserServiceVajram.ID))
            // If this dependency fails, fail this Vajram
            .isMandatory()
            .build(),
        Input.builder()
            .name("log")
            .type(java(Logger.class))
            // This is not expected from clients
            // This is expected to be injected by the runtime
            .sources(InputSource.SESSION)
            .build(),
        Input.builder()
            // Data type of resolved dependencies is inferred from the
            // dependency Vajram's Definition
            .name("analytics_event_sink")
            .type(java(AnalyticsEventSink.class))
            .sources(InputSource.SESSION)
            .build());
  }

  @Override
  public DependencyCommand<Inputs> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
    switch (dependency) {
      case "user_info" -> {
        if (Set.of("user_id").equals(resolvableInputs)) {
          String userId = super.userIdForUserService(inputs.getInputValueOrThrow("user_id"));
          return DependencyCommand.executeWith(
              new Inputs(ImmutableMap.of("user_id", ValueOrError.withValue(userId))));
        }
      }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public ImmutableMap<Inputs, ValueOrError<String>> executeCompute(
      ImmutableList<Inputs> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                i -> i,
                i -> {
                  ImmutableMap<Inputs, ValueOrError<UserInfo>> userInfo =
                      i.getInputValueOrThrow("user_info");
                  DependencyResponse<UserServiceRequest, UserInfo> userInfoResponse =
                      new DependencyResponse<>(
                          userInfo.entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> UserServiceRequest.from(e.getKey()), Entry::getValue)));
                  return valueOrError(
                      () ->
                          createGreetingMessage(
                              new AllInputs(
                                  i.getInputValueOrThrow("user_id"),
                                  i.getInputValueOrDefault("log", null),
                                  i.getInputValueOrDefault("analytics_event_sink", null),
                                  userInfoResponse)));
                }));
  }
}

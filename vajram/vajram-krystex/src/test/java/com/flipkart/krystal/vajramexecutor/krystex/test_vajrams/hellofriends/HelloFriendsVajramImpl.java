package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import static com.flipkart.krystal.data.ValueOrError.valueOrError;
import static com.flipkart.krystal.datatypes.IntegerType.integer;
import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsInputUtil.HelloFriendsAllInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public final class HelloFriendsVajramImpl extends HelloFriendsVajram {

  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name(USER_ID).type(string()).isMandatory().build(),
        Input.builder().name(NUMBER_OF_FRIENDS).type(integer()).build(),
        Dependency.builder()
            .name(USER_INFOS)
            .dataAccessSpec(vajramID(TestUserServiceVajram.ID))
            .isMandatory()
            .build(),
        Dependency.builder()
            .name(FRIEND_INFOS)
            .dataAccessSpec(vajramID(TestUserServiceVajram.ID))
            .isMandatory()
            .build());
  }

  @Override
  public DependencyCommand<Inputs> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
    String userId = inputs.getInputValueOrThrow("user_id");
    switch (dependency) {
      case USER_INFOS:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            return DependencyCommand.executeWith(
                new Inputs(
                    ImmutableMap.of(
                        "user_id", ValueOrError.withValue(userIdForUserService(userId)))));
          }
        }
      case FRIEND_INFOS:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            Optional<Integer> numberOfFriends = inputs.getInputValueOpt("number_of_friends");
            if (numberOfFriends.isPresent()) {
              return DependencyCommand.multiExecuteWith(
                  friendIdsForUserService(userId, numberOfFriends.get()).stream()
                      .map(s -> new Inputs(ImmutableMap.of("user_id", ValueOrError.withValue(s))))
                      .collect(toImmutableList()));
            }
          }
        }
    }
    throw new IllegalArgumentException();
  }

  public ImmutableMap<Inputs, ValueOrError<String>> executeCompute(
      ImmutableList<Inputs> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                identity(),
                inputs -> {
                  Results<TestUserInfo> userInfo = inputs.getDepValue("user_infos");
                  DependencyResponse<TestUserServiceRequest, TestUserInfo> userInfoResponse =
                      new DependencyResponse<>(
                          userInfo.values().entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> TestUserServiceRequest.from(e.getKey()),
                                      Entry::getValue)));

                  Results<TestUserInfo> friendInfos = inputs.getDepValue("friend_infos");
                  DependencyResponse<TestUserServiceRequest, TestUserInfo> friendInfosResponse =
                      new DependencyResponse<>(
                          friendInfos.values().entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> TestUserServiceRequest.from(e.getKey()),
                                      Entry::getValue)));
                  return valueOrError(
                      () ->
                          sayHellos(
                              new HelloFriendsAllInputs(
                                  inputs.<String>getInputValue("user_id").value().orElseThrow(),
                                  inputs
                                      .<Integer>getInputValue("number_of_friends")
                                      .value()
                                      .orElse(null),
                                  userInfoResponse,
                                  friendInfosResponse)));
                }));
  }
}

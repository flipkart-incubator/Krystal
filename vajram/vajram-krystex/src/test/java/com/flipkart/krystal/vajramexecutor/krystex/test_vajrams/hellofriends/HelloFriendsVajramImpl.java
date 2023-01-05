package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

import static com.flipkart.krystal.datatypes.IntegerType.integer;
import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.ValueOrError;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsInputUtil.AllInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public class HelloFriendsVajramImpl extends HelloFriendsVajram {

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
  public ImmutableList<InputValues> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    String userId = executionContext.getValue("user_id");
    Optional<Integer> numberOfFriends = executionContext.optValue("number_of_friends");
    switch (dependency) {
      case USER_INFOS:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            return ImmutableList.of(
                new InputValues(
                    ImmutableMap.of("user_id", new ValueOrError<>(userIdForUserService(userId)))));
          }
        }
      case FRIEND_INFOS:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            if (numberOfFriends.isPresent()) {
              return friendIdsForUserService(userId, numberOfFriends.get()).stream()
                  .map(
                      s -> new InputValues(ImmutableMap.of("user_id", new ValueOrError<Object>(s))))
                  .collect(toImmutableList());
            } else {
              return ImmutableList.of(new InputValues());
            }
          }
        }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public ImmutableMap<InputValues, ImmutableList<String>> executeCompute(
      ImmutableList<InputValues> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                i -> i,
                i -> {
                  ImmutableMap<InputValues, ValueOrError<TestUserInfo>> userInfo =
                      i.getOrThrow("user_infos");
                  DependencyResponse<TestUserServiceRequest, TestUserInfo> userInfoResponse =
                      new DependencyResponse<>(
                          userInfo.entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> TestUserServiceRequest.from(e.getKey()),
                                      Entry::getValue)));

                  ImmutableMap<InputValues, ValueOrError<TestUserInfo>> friendInfos =
                      i.getOrThrow("friend_infos");
                  DependencyResponse<TestUserServiceRequest, TestUserInfo> friendInfosResponse =
                      new DependencyResponse<>(
                          friendInfos.entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> TestUserServiceRequest.from(e.getKey()),
                                      Entry::getValue)));

                  try {
                    return ImmutableList.of(
                        sayHellos(
                            new AllInputs(
                                i.getOrThrow("user_id"),
                                i.getOrDefault("number_of_friends", null),
                                userInfoResponse,
                                friendInfosResponse)));
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                }));
  }
}

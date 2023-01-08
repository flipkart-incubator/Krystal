package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2InputUtil.AllInputs;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public class HelloFriendsV2VajramImpl extends HelloFriendsV2Vajram {
  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name(USER_ID).type(string()).isMandatory().build(),
        Dependency.builder()
            .name(FRIEND_IDS)
            .dataAccessSpec(vajramID(FriendsServiceVajram.ID))
            .isMandatory(true)
            .build(),
        Dependency.builder()
            .name(FRIEND_INFOS)
            .dataAccessSpec(vajramID(TestUserServiceVajram.ID))
            .isMandatory(true)
            .build());
  }

  @Override
  public DependencyCommand<InputValues> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, InputValues inputValues) {
    Optional<String> userId = inputValues.<String>getValue("user_id").value();
    switch (dependency) {
      case FRIEND_IDS:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            if (userId.isPresent()) {
              return DependencyCommand.executeWith(
                  new InputValues(
                      ImmutableMap.of(
                          "user_id",
                          new ValueOrError<Object>(userIdForFriendService(userId.get())))));
            }
          }
        }
      case FRIEND_INFOS:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            DependencyResponse<FriendsServiceRequest, Set<String>> friendIds =
                new DependencyResponse<>(
                    inputValues
                        .<ImmutableMap<InputValues, ValueOrError<Set<String>>>>getOrThrow(
                            FRIEND_IDS)
                        .entrySet()
                        .stream()
                        .collect(
                            toImmutableMap(
                                e -> FriendsServiceRequest.from(e.getKey()), Entry::getValue)));
            Set<String> userIdsForUserService = userIdsForUserService(friendIds);
            return DependencyCommand.multiExecuteWith(
                userIdsForUserService.stream()
                    .map(s -> new InputValues(ImmutableMap.of("user_id", new ValueOrError<>(s))))
                    .collect(toImmutableList()));
          }
        }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public ImmutableMap<InputValues, String> executeCompute(ImmutableList<InputValues> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                i -> i,
                i -> {
                  ImmutableMap<InputValues, ValueOrError<LinkedHashSet<String>>> friendIds =
                      i.getOrThrow(FRIEND_IDS);
                  DependencyResponse<FriendsServiceRequest, LinkedHashSet<String>>
                      friendIdsResponse =
                          new DependencyResponse<>(
                              friendIds.entrySet().stream()
                                  .collect(
                                      toImmutableMap(
                                          e1 -> FriendsServiceRequest.from(e1.getKey()),
                                          Entry::getValue)));
                  ImmutableMap<InputValues, ValueOrError<TestUserInfo>> friendInfos =
                      i.getOrThrow(FRIEND_INFOS);
                  DependencyResponse<TestUserServiceRequest, TestUserInfo> friendInfosResponse =
                      new DependencyResponse<>(
                          friendInfos.entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> TestUserServiceRequest.from(e.getKey()),
                                      Entry::getValue)));

                  return sayHellos(
                      new AllInputs(
                          i.getOrThrow("user_id"), friendIdsResponse, friendInfosResponse));
                }));
  }
}

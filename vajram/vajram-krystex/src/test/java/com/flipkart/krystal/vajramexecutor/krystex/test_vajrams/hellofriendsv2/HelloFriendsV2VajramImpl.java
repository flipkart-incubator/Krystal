package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Input;
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
  public DependencyCommand<Inputs> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
    Optional<String> userId = inputs.<String>getInputValue("user_id").value();
    switch (dependency) {
      case FRIEND_IDS:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            if (userId.isPresent()) {
              return DependencyCommand.executeWith(
                  new Inputs(
                      ImmutableMap.of(
                          "user_id",
                          ValueOrError.withValue(userIdForFriendService(userId.get())))));
            }
          }
        }
      case FRIEND_INFOS:
        {
          if (Set.of("user_id").equals(resolvableInputs)) {
            DependencyResponse<FriendsServiceRequest, Set<String>> friendIds =
                new DependencyResponse<>(
                    inputs.<Set<String>>getDepValue(FRIEND_IDS).values().entrySet().stream()
                        .collect(
                            toImmutableMap(
                                e -> FriendsServiceRequest.from(e.getKey()), Entry::getValue)));
            Set<String> userIdsForUserService = userIdsForUserService(friendIds);
            return DependencyCommand.multiExecuteWith(
                userIdsForUserService.stream()
                    .map(s -> new Inputs(ImmutableMap.of("user_id", ValueOrError.withValue(s))))
                    .collect(toImmutableList()));
          }
        }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public ImmutableMap<Inputs, String> executeCompute(ImmutableList<Inputs> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                i -> i,
                i -> {
                  Results<Set<String>> friendIds = i.getDepValue(FRIEND_IDS);
                  DependencyResponse<FriendsServiceRequest, Set<String>> friendIdsResponse =
                      new DependencyResponse<>(
                          friendIds.values().entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> FriendsServiceRequest.from(e.getKey()),
                                      Entry::getValue)));
                  Results<TestUserInfo> friendInfos = i.getDepValue(FRIEND_INFOS);
                  DependencyResponse<TestUserServiceRequest, TestUserInfo> friendInfosResponse =
                      new DependencyResponse<>(
                          friendInfos.values().entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> TestUserServiceRequest.from(e.getKey()),
                                      Entry::getValue)));

                  return sayHellos(
                      new AllInputs(
                          i.getInputValueOrThrow("user_id"),
                          friendIdsResponse,
                          friendInfosResponse));
                }));
  }
}

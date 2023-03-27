package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2;

public final class HelloFriendsV2VajramImpl123 {
  //  @Override
  //  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
  //    return ImmutableList.of(
  //        Input.builder()
  //            .name(HelloFriendsV2Vajram.USER_ID)
  //            .type(StringType.string())
  //            .isMandatory()
  //            .build(),
  //        Dependency.builder()
  //            .name(FRIEND_IDS)
  //            .dataAccessSpec(vajramID(FriendsServiceVajram.ID))
  //            .isMandatory(true)
  //            .build(),
  //        Dependency.builder()
  //            .name(FRIEND_INFOS)
  //            .dataAccessSpec(vajramID(TestUserServiceVajram.ID))
  //            .isMandatory(true)
  //            .build());
  //  }
  //
  //  @Override
  //  public DependencyCommand<Inputs> resolveInputOfDependency(
  //      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
  //    Optional<String> userId = inputs.<String>getInputValue("user_id").value();
  //    switch (dependency) {
  //      case FRIEND_IDS:
  //        {
  //          if (Set.of("user_id").equals(resolvableInputs)) {
  //            if (userId.isPresent()) {
  //              return DependencyCommand.executeWith(
  //                  new Inputs(
  //                      ImmutableMap.of(
  //                          "user_id",
  //                          ValueOrError.withValue(userIdForFriendService(userId.get())))));
  //            }
  //          }
  //        }
  //      case FRIEND_INFOS:
  //        {
  //          if (Set.of("user_id").equals(resolvableInputs)) {
  //            DependencyResponse<FriendsServiceRequest, Set<String>> friendIds =
  //                new DependencyResponse<>(
  //                    inputs.<Set<String>>getDepValue(FRIEND_IDS).values().entrySet().stream()
  //                        .collect(
  //                            toImmutableMap(
  //                                e -> FriendsServiceRequest.from(e.getKey()), Entry::getValue)));
  //            Set<String> userIdsForUserService = userIdsForUserService(friendIds);
  //            return DependencyCommand.multiExecuteWith(
  //                userIdsForUserService.stream()
  //                    .map(s -> new Inputs(ImmutableMap.of("user_id", ValueOrError.withValue(s))))
  //                    .collect(toImmutableList()));
  //          }
  //        }
  //    }
  //    throw new IllegalArgumentException();
  //  }
  //
  //  @Override
  //  public ImmutableMap<Inputs, ValueOrError<String>> executeCompute(
  //      ImmutableList<Inputs> inputsList) {
  //    return inputsList.stream()
  //        .collect(
  //            toImmutableMap(
  //                i -> i,
  //                i -> {
  //                  Results<Set<String>> friendIds = i.getDepValue(FRIEND_IDS);
  //                  DependencyResponse<FriendsServiceRequest, Set<String>> friendIdsResponse =
  //                      new DependencyResponse<>(
  //                          friendIds.values().entrySet().stream()
  //                              .collect(
  //                                  toImmutableMap(
  //                                      e -> FriendsServiceRequest.from(e.getKey()),
  //                                      Entry::getValue)));
  //                  Results<TestUserInfo> friendInfos = i.getDepValue(FRIEND_INFOS);
  //                  DependencyResponse<TestUserServiceRequest, TestUserInfo> friendInfosResponse =
  //                      new DependencyResponse<>(
  //                          friendInfos.values().entrySet().stream()
  //                              .collect(
  //                                  toImmutableMap(
  //                                      e -> TestUserServiceRequest.from(e.getKey()),
  //                                      Entry::getValue)));
  //
  //                  return valueOrError(
  //                      () ->
  //                          sayHellos(
  //                              new HelloFriendsV2AllInputs(
  //                                  i.getInputValueOrThrow("user_id"),
  //                                  friendIdsResponse,
  //                                  friendInfosResponse)));
  //                }));
  //  }
}

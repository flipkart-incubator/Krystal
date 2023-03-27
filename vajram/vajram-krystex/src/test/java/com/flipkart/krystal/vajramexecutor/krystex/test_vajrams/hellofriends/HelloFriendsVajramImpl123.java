package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends;

public final class HelloFriendsVajramImpl123 {

  //  @Override
  //  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
  //    return ImmutableList.of(
  //        Input.builder().name(USER_ID).type(string()).isMandatory().build(),
  //        Input.builder().name(NUMBER_OF_FRIENDS).type(integer()).build(),
  //        Dependency.builder()
  //            .name(USER_INFOS)
  //            .dataAccessSpec(vajramID(TestUserServiceVajram.ID))
  //            .isMandatory()
  //            .build(),
  //        Dependency.builder()
  //            .name(FRIEND_INFOS)
  //            .dataAccessSpec(vajramID(TestUserServiceVajram.ID))
  //            .isMandatory()
  //            .build());
  //  }
  //
  //  @Override
  //  public DependencyCommand<Inputs> resolveInputOfDependency(
  //      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
  //    String userId = inputs.getInputValueOrThrow("user_id");
  //    switch (dependency) {
  //      case USER_INFOS:
  //        {
  //          if (Set.of("user_id").equals(resolvableInputs)) {
  //            return DependencyCommand.executeWith(
  //                new Inputs(
  //                    ImmutableMap.of(
  //                        "user_id", ValueOrError.withValue(userIdForUserService(userId)))));
  //          }
  //        }
  //      case FRIEND_INFOS:
  //        {
  //          if (Set.of("user_id").equals(resolvableInputs)) {
  //            Optional<Integer> numberOfFriends = inputs.getInputValueOpt("number_of_friends");
  //            if (numberOfFriends.isPresent()) {
  //              return DependencyCommand.multiExecuteWith(
  //                  friendIdsForUserService(userId, numberOfFriends.get()).stream()
  //                      .map(s -> new Inputs(ImmutableMap.of("user_id",
  // ValueOrError.withValue(s))))
  //                      .collect(toImmutableList()));
  //            }
  //          }
  //        }
  //    }
  //    throw new IllegalArgumentException();
  //  }
  //
  //  public ImmutableMap<Inputs, ValueOrError<String>> executeCompute(
  //      ImmutableList<Inputs> inputsList) {
  //    return inputsList.stream()
  //        .collect(
  //            toImmutableMap(
  //                identity(),
  //                inputs -> {
  //                  Results<TestUserInfo> userInfo = inputs.getDepValue("user_infos");
  //                  DependencyResponse<TestUserServiceRequest, TestUserInfo> userInfoResponse =
  //                      new DependencyResponse<>(
  //                          userInfo.values().entrySet().stream()
  //                              .collect(
  //                                  toImmutableMap(
  //                                      e -> TestUserServiceRequest.from(e.getKey()),
  //                                      Entry::getValue)));
  //
  //                  Results<TestUserInfo> friendInfos = inputs.getDepValue("friend_infos");
  //                  DependencyResponse<TestUserServiceRequest, TestUserInfo> friendInfosResponse =
  //                      new DependencyResponse<>(
  //                          friendInfos.values().entrySet().stream()
  //                              .collect(
  //                                  toImmutableMap(
  //                                      e -> TestUserServiceRequest.from(e.getKey()),
  //                                      Entry::getValue)));
  //                  return valueOrError(
  //                      () ->
  //                          sayHellos(
  //                              new HelloFriendsAllInputs(
  //                                  inputs.<String>getInputValue("user_id").value().orElseThrow(),
  //                                  inputs
  //                                      .<Integer>getInputValue("number_of_friends")
  //                                      .value()
  //                                      .orElse(null),
  //                                  userInfoResponse,
  //                                  friendInfosResponse)));
  //                }));
  //  }
}

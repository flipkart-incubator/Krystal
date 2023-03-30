package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2;

public final class MultiHelloFriendsV2Impl123 {

  //  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
  //    return ImmutableList.of(
  //        Input.builder().name("user_ids").type(list(string())).isMandatory().build(),
  //        Input.builder().name("skip").type(bool()).build(),
  //        Dependency.builder()
  //            .name("hellos")
  //            .dataAccessSpec(vajramID(HelloFriendsV2Vajram.ID))
  //            .isMandatory()
  //            .build());
  //  }
  //
  //  public DependencyCommand<Inputs> resolveInputOfDependency(
  //      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
  //    if (dependency.equals("hellos")) {
  //      if (Set.of("user_id").equals(resolvableInputs)) {
  //        Set<String> userIds = inputs.getInputValueOrThrow("user_ids");
  //        Optional<Boolean> skip = inputs.getInputValueOpt("skip");
  //        DependencyCommand<String> depCommand = MultiHelloFriendsV2.userIdsForHellos(userIds,
  // skip);
  //        if (depCommand instanceof DependencyCommand.Skip<String>) {
  //          return ((Skip<String>) depCommand).cast();
  //        } else {
  //          return DependencyCommand.multiExecuteWith(
  //              depCommand.inputs().stream()
  //                  .map(s -> new Inputs(ImmutableMap.of("user_id", ValueOrError.withValue(s))))
  //                  .collect(toImmutableList()));
  //        }
  //      }
  //    }
  //    throw new UnsupportedOperationException();
  //  }
  //
  //  public ImmutableMap<Inputs, ValueOrError<String>> executeCompute(
  //      ImmutableList<Inputs> inputsList) {
  //    return inputsList.stream()
  //        .collect(
  //            toImmutableMap(
  //                identity(),
  //                inputValues -> {
  //                  LinkedHashSet<String> userIds = inputValues.getInputValueOrThrow("user_ids");
  //                  Results<String> hellosResult = inputValues.getDepValue("hellos");
  //                  DependencyResponse<HelloFriendsV2Request, String> hellos =
  //                      new DependencyResponse<>(
  //                          hellosResult.values().entrySet().stream()
  //                              .collect(
  //                                  toImmutableMap(
  //                                      e -> HelloFriendsV2Request.from(e.getKey()),
  //                                      Entry::getValue)));
  //                  return valueOrError(
  //                      () ->
  //                          MultiHelloFriendsV2.sayHellos(
  //                              new MultiHelloFriendsV2AllInputs(userIds, false, hellos)));
  //                }));
  //  }
}

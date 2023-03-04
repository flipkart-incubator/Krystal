package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello;

import static com.flipkart.krystal.datatypes.ListType.list;
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
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsInputUtil.MultiHelloFriendsAllInputs;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

public final class MultiHelloFriendsImpl extends MultiHelloFriends {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("user_ids").type(list(string())).isMandatory().build(),
        Dependency.builder()
            .name("hellos")
            .dataAccessSpec(vajramID(HelloFriendsVajram.ID))
            .isMandatory()
            .build());
  }

  @Override
  public DependencyCommand<Inputs> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
    if (dependency.equals("hellos")) {
      if (Set.of("user_id").equals(resolvableInputs)) {
        List<String> userIds = inputs.getInputValueOrThrow("user_ids");
        return DependencyCommand.multiExecuteWith(
            userIdsForHellos(userIds).stream()
                .map(s -> new Inputs(ImmutableMap.of("user_id", ValueOrError.withValue(s))))
                .collect(toImmutableList()));
      }
      if (Set.of("number_of_friends").equals(resolvableInputs)) {
        return DependencyCommand.multiExecuteWith(
            numberOfFriendsForHellos(inputs.getInputValueOrThrow("user_ids")).stream()
                .map(
                    s ->
                        new Inputs(ImmutableMap.of("number_of_friends", ValueOrError.withValue(s))))
                .collect(toImmutableList()));
      }
    }
    throw new UnsupportedOperationException();
  }

  public ImmutableMap<Inputs, ValueOrError<String>> executeCompute(
      ImmutableList<Inputs> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                Function.identity(),
                inputValues -> {
                  ArrayList<String> userIds = inputValues.getInputValueOrThrow("user_ids");
                  Results<String> hellosResult = inputValues.getDepValue("hellos");
                  DependencyResponse<HelloFriendsRequest, String> hellos =
                      new DependencyResponse<>(
                          hellosResult.values().entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> HelloFriendsRequest.from(e.getKey()), Entry::getValue)));
                  return ValueOrError.valueOrError(
                      () -> sayHellos(new MultiHelloFriendsAllInputs(userIds, hellos)));
                }));
  }
}

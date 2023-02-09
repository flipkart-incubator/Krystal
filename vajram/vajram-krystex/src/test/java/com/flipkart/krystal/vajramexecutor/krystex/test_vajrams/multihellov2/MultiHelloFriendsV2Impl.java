package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2;

import static com.flipkart.krystal.data.ValueOrError.valueOrError;
import static com.flipkart.krystal.datatypes.ListType.list;
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
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Vajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2InputUtil.AllInputs;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

public final class MultiHelloFriendsV2Impl extends MultiHelloFriendsV2 {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("user_ids").type(list(string())).isMandatory().build(),
        Dependency.builder()
            .name("hellos")
            .dataAccessSpec(vajramID(HelloFriendsV2Vajram.ID))
            .isMandatory()
            .build());
  }

  @Override
  public DependencyCommand<Inputs> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
    if (dependency.equals("hellos")) {
      if (Set.of("user_id").equals(resolvableInputs)) {
        Set<String> userIds = inputs.getInputValueOrThrow("user_ids");
        return DependencyCommand.multiExecuteWith(
            userIdsForHellos(userIds).stream()
                .map(s -> new Inputs(ImmutableMap.of("user_id", ValueOrError.withValue(s))))
                .collect(toImmutableList()));
      }
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableMap<Inputs, ValueOrError<String>> executeCompute(
      ImmutableList<Inputs> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                identity(),
                inputValues -> {
                  LinkedHashSet<String> userIds = inputValues.getInputValueOrThrow("user_ids");
                  Results<String> hellosResult = inputValues.getDepValue("hellos");
                  DependencyResponse<HelloFriendsV2Request, String> hellos =
                      new DependencyResponse<>(
                          hellosResult.values().entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> HelloFriendsV2Request.from(e.getKey()),
                                      Entry::getValue)));
                  return valueOrError(() -> sayHellos(new AllInputs(userIds, hellos)));
                }));
  }
}

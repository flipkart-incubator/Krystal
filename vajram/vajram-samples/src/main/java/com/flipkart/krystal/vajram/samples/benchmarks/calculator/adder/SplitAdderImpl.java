package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.flipkart.krystal.datatypes.IntegerType.integer;
import static com.flipkart.krystal.datatypes.ListType.list;
import static com.flipkart.krystal.vajram.inputs.DependencyCommand.multiExecuteWith;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.SplitAdderInputUtil.AllInputs;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public final class SplitAdderImpl extends SplitAdder {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("numbers").isMandatory().type(list(integer())).build(),
        Dependency.builder()
            .name("split_sum_1")
            .dataAccessSpec(VajramID.vajramID("splitAdder"))
            .build(),
        Dependency.builder()
            .name("split_sum_2")
            .dataAccessSpec(VajramID.vajramID("splitAdder"))
            .build(),
        Dependency.builder().name("sum").dataAccessSpec(VajramID.vajramID("adder")).build());
  }

  @Override
  public DependencyCommand<Inputs> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
    switch (dependency) {
      case "split_sum_1" -> {
        if (Set.of("numbers").equals(resolvableInputs)) {
          ArrayList<Integer> numbers = inputs.getInputValueOrThrow("numbers");
          DependencyCommand<ArrayList<Integer>> depCommand = numbersForSubSplitter1(numbers);
          if (depCommand instanceof DependencyCommand.Skip<ArrayList<Integer>> skip) {
            return skip.cast();
          } else {
            return multiExecuteWith(
                depCommand.inputs().stream()
                    .map(
                        integers ->
                            new Inputs(
                                ImmutableMap.of("numbers", ValueOrError.withValue(integers))))
                    .toList());
          }
        }
      }
      case "split_sum_2" -> {
        if (Set.of("numbers").equals(resolvableInputs)) {
          ArrayList<Integer> numbers = inputs.getInputValueOrThrow("numbers");
          DependencyCommand<ArrayList<Integer>> depCommand = numbersForSubSplitter2(numbers);
          if (depCommand instanceof DependencyCommand.Skip<ArrayList<Integer>> skip) {
            return skip.cast();
          } else {
            return multiExecuteWith(
                depCommand.inputs().stream()
                    .map(
                        integers ->
                            new Inputs(
                                ImmutableMap.of("numbers", ValueOrError.withValue(integers))))
                    .toList());
          }
        }
      }
      case "sum" -> {
        if (Set.of("number_one").equals(resolvableInputs)) {
          ArrayList<Integer> numbers = inputs.getInputValueOrThrow("numbers");
          DependencyCommand<Integer> depCommand = adderNumberOne(numbers);
          if (depCommand instanceof DependencyCommand.Skip<Integer> skip) {
            return skip.cast();
          } else {
            return multiExecuteWith(
                depCommand.inputs().stream()
                    .map(
                        integer ->
                            new Inputs(
                                ImmutableMap.of("number_one", ValueOrError.withValue(integer))))
                    .toList());
          }
        }
        if (Set.of("number_two").equals(resolvableInputs)) {
          ArrayList<Integer> numbers = inputs.getInputValueOrThrow("numbers");
          Integer value = adderNumberTwo(numbers);
          DependencyCommand<Integer> depCommand =
              Optional.ofNullable(value)
                  .map(DependencyCommand::executeWith)
                  .orElse(DependencyCommand.executeWith(null));
          return multiExecuteWith(
              depCommand.inputs().stream()
                  .map(
                      integer ->
                          new Inputs(
                              ImmutableMap.of("number_two", ValueOrError.withValue(integer))))
                  .toList());
        }
      }
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Integer executeCompute(Inputs inputValues) {
    ArrayList<Integer> numbers = inputValues.getInputValueOrThrow("numbers");
    Map<Inputs, ValueOrError<Integer>> splitSum1Result =
        inputValues.<Integer>getDepValue("split_sum_1").values();
    DependencyResponse<SplitAdderRequest, Integer> splitSum1 =
        new DependencyResponse<>(
            splitSum1Result.entrySet().stream()
                .collect(toImmutableMap(e -> SplitAdderRequest.from(e.getKey()), Entry::getValue)));
    Map<Inputs, ValueOrError<Integer>> splitSum2Result =
        inputValues.<Integer>getDepValue("split_sum_2").values();
    DependencyResponse<SplitAdderRequest, Integer> splitSum2 =
        new DependencyResponse<>(
            splitSum2Result.entrySet().stream()
                .collect(toImmutableMap(e -> SplitAdderRequest.from(e.getKey()), Entry::getValue)));
    Map<Inputs, ValueOrError<Integer>> sumResult = inputValues.<Integer>getDepValue("sum").values();
    DependencyResponse<AdderRequest, Integer> sum =
        new DependencyResponse<>(
            sumResult.entrySet().stream()
                .collect(toImmutableMap(e -> AdderRequest.from(e.getKey()), Entry::getValue)));
    return add(new AllInputs(numbers, splitSum1, splitSum2, sum));
  }
}

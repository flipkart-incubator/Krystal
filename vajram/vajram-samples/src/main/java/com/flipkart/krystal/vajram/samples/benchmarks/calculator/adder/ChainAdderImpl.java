package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.flipkart.krystal.datatypes.IntegerType.integer;
import static com.flipkart.krystal.datatypes.ListType.list;
import static com.flipkart.krystal.vajram.inputs.DependencyCommand.multiExecuteWith;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.data.InputValues;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.ChainAdderInputUtil.AllInputs;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class ChainAdderImpl extends ChainAdder {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("numbers").isMandatory().type(list(integer())).build(),
        Dependency.builder()
            .name("chain_sum")
            .dataAccessSpec(VajramID.vajramID("chainAdder"))
            .build(),
        Dependency.builder().name("sum").dataAccessSpec(VajramID.vajramID("adder")).build());
  }

  @Override
  public DependencyCommand<InputValues> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, InputValues inputValues) {
    switch (dependency) {
      case "chain_sum" -> {
        if (Set.of("numbers").equals(resolvableInputs)) {
          ArrayList<Integer> numbers = inputValues.getOrThrow("numbers");
          DependencyCommand<ArrayList<Integer>> depCommand = numbersForSubChainer(numbers);
          if (depCommand instanceof DependencyCommand.Skip<ArrayList<Integer>> skip) {
            return skip.cast();
          } else {
            return multiExecuteWith(
                depCommand.inputs().stream()
                    .map(
                        integers ->
                            new InputValues(
                                ImmutableMap.of("numbers", new ValueOrError<>(integers))))
                    .toList());
          }
        }
      }
      case "sum" -> {
        if (Set.of("number_one").equals(resolvableInputs)) {
          ArrayList<Integer> numbers = inputValues.getOrThrow("numbers");
          DependencyCommand<Integer> depCommand = adderNumberOne(numbers);
          if (depCommand instanceof DependencyCommand.Skip<Integer> skip) {
            return skip.cast();
          } else {
            return multiExecuteWith(
                depCommand.inputs().stream()
                    .map(
                        integer ->
                            new InputValues(
                                ImmutableMap.of("number_one", new ValueOrError<>(integer))))
                    .toList());
          }
        }
        if (Set.of("number_two").equals(resolvableInputs)) {
          ArrayList<Integer> numbers = inputValues.getOrThrow("numbers");
          DependencyCommand<Integer> depCommand =
              Optional.ofNullable(adderNumberTwo(numbers))
                  .orElse(DependencyCommand.executeWith(null));
          if (depCommand instanceof DependencyCommand.Skip<Integer> skip) {
            return skip.cast();
          } else {
            return multiExecuteWith(
                depCommand.inputs().stream()
                    .map(
                        integer ->
                            new InputValues(
                                ImmutableMap.of("number_two", new ValueOrError<>(integer))))
                    .toList());
          }
        }
      }
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableMap<InputValues, Integer> executeCompute(ImmutableList<InputValues> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                Function.identity(),
                inputValues -> {
                  ArrayList<Integer> numbers = inputValues.getOrThrow("numbers");
                  ImmutableMap<InputValues, ValueOrError<Integer>> chainSumResult =
                      inputValues.getOrThrow("chain_sum");
                  DependencyResponse<ChainAdderRequest, Integer> chainSum =
                      new DependencyResponse<>(
                          chainSumResult.entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> ChainAdderRequest.from(e.getKey()), Entry::getValue)));
                  ImmutableMap<InputValues, ValueOrError<Integer>> sumResult =
                      inputValues.getOrThrow("sum");
                  DependencyResponse<AdderRequest, Integer> sum =
                      new DependencyResponse<>(
                          sumResult.entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> AdderRequest.from(e.getKey()), Entry::getValue)));
                  return add(new AllInputs(numbers, chainSum, sum));
                }));
  }
}

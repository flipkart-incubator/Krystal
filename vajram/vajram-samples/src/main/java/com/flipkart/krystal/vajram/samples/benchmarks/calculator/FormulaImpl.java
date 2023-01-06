package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.datatypes.IntegerType;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.ValueOrError;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaInputUtil.AllInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

public class FormulaImpl extends Formula {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("a").type(IntegerType.integer()).isMandatory().build(),
        Input.builder().name("p").type(IntegerType.integer()).isMandatory().build(),
        Input.builder().name("q").type(IntegerType.integer()).isMandatory().build(),
        Dependency.builder()
            .name("sum")
            .isMandatory()
            .dataAccessSpec(VajramID.vajramID("adder"))
            .build(),
        Dependency.builder()
            .name("quotient")
            .isMandatory()
            .dataAccessSpec(VajramID.vajramID("divider"))
            .build());
  }

  @Override
  public ImmutableList<InputValues> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    switch (dependency) {
      case "sum" -> {
        if (Set.of("number_one").equals(resolvableInputs)) {
          return ImmutableList.of(
              new InputValues(
                  ImmutableMap.of(
                      "number_one",
                      new ValueOrError<>(adderNumberOne(executionContext.getValue("p"))))));
        }
        if (Set.of("number_two").equals(resolvableInputs)) {
          return ImmutableList.of(
              new InputValues(
                  ImmutableMap.of(
                      "number_two",
                      new ValueOrError<>(adderNumberOne(executionContext.getValue("q"))))));
        }
      }
      case "quotient" -> {
        if (Set.of("number_one").equals(resolvableInputs)) {
          return ImmutableList.of(
              new InputValues(
                  ImmutableMap.of(
                      "number_one",
                      new ValueOrError<>(quotientNumberOne(executionContext.getValue("a"))))));
        }
        if (Set.of("number_two").equals(resolvableInputs)) {
          DependencyResponse<AdderRequest, Integer> sumResponses =
              new DependencyResponse<>(
                  executionContext
                      .context()
                      .<ImmutableMap<InputValues, ValueOrError<Integer>>>getOrThrow("sum")
                      .entrySet()
                      .stream()
                      .collect(
                          toImmutableMap(e -> AdderRequest.from(e.getKey()), Entry::getValue)));
          return sumResponses.values().stream()
              .filter(voe -> voe.value().isPresent())
              .map(voe -> voe.value().get())
              .map(Formula::quotientNumberTwo)
              .map(ValueOrError::new)
              .map(voe -> new InputValues(ImmutableMap.of("number_two", voe)))
              .collect(ImmutableList.toImmutableList());
        }
      }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public ImmutableMap<InputValues, ImmutableList<Integer>> executeCompute(
      ImmutableList<InputValues> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                Function.identity(),
                iv -> {
                  DependencyResponse<AdderRequest, Integer> sumResponses =
                      new DependencyResponse<>(
                          iv
                              .<ImmutableMap<InputValues, ValueOrError<Integer>>>getOrThrow("sum")
                              .entrySet()
                              .stream()
                              .collect(
                                  toImmutableMap(
                                      e -> AdderRequest.from(e.getKey()), Entry::getValue)));
                  DependencyResponse<DividerRequest, Integer> quotientResponse =
                      new DependencyResponse<>(
                          iv
                              .<ImmutableMap<InputValues, ValueOrError<Integer>>>getOrThrow(
                                  "quotient")
                              .entrySet()
                              .stream()
                              .collect(
                                  toImmutableMap(
                                      e -> DividerRequest.from(e.getKey()), Entry::getValue)));
                  return ImmutableList.of(
                      result(
                          new AllInputs(
                              iv.getOrThrow("a"),
                              iv.getOrThrow("p"),
                              iv.getOrThrow("q"),
                              sumResponses,
                              quotientResponse)));
                }));
  }
}

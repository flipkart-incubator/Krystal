package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.datatypes.IntegerType;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.FormulaInputUtil.FormulaAllInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

public final class FormulaImpl123 {

  //    @Override
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

  //    @Override
  public DependencyCommand<Inputs> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
    switch (dependency) {
      case "sum" -> {
        if (Set.of("number_one").equals(resolvableInputs)) {
          return DependencyCommand.executeWith(
              new Inputs(
                  ImmutableMap.of(
                      "number_one",
                      ValueOrError.withValue(
                          Formula.adderNumberOne(inputs.getInputValueOrThrow("p"))))));
        }
        if (Set.of("number_two").equals(resolvableInputs)) {
          return DependencyCommand.executeWith(
              new Inputs(
                  ImmutableMap.of(
                      "number_two",
                      ValueOrError.withValue(
                          Formula.adderNumberTwo(inputs.getInputValueOrThrow("q"))))));
        }
      }
      case "quotient" -> {
        if (Set.of("number_one").equals(resolvableInputs)) {
          return DependencyCommand.executeWith(
              new Inputs(
                  ImmutableMap.of(
                      "number_one",
                      ValueOrError.withValue(
                          Formula.quotientNumberOne(inputs.getInputValueOrThrow("a"))))));
        }
        if (Set.of("number_two").equals(resolvableInputs)) {
          final Map<Inputs, ValueOrError<Integer>> sum =
              inputs.<Integer>getDepValue("sum").values();
          return DependencyCommand.executeWith(
              new Inputs(
                  ImmutableMap.of(
                      "number_two",
                      ValueOrError.withValue(
                          Formula.quotientNumberTwo(
                              sum.values().iterator().next().value().orElse(null))))));

          //          DependencyResponse<AdderRequest, Integer> sumResponses =
          //              new DependencyResponse<>(
          //                  inputs.<Integer>getDepValue("sum").values().entrySet().stream()
          //                      .collect(
          //                          toImmutableMap(e -> AdderRequest.from(e.getKey()),
          // Entry::getValue)));
          //          return DependencyCommand.multiExecuteWith(
          //              sumResponses.values().stream()
          //                  .filter(voe -> voe.value().isPresent())
          //                  .map(voe -> voe.value().get())
          //                  .map(Formula::quotientNumberTwo)
          //                  .map(t -> ValueOrError.withValue((Object) t))
          //                  .map(voe -> new Inputs(ImmutableMap.of("number_two", voe)))
          //                  .collect(ImmutableList.toImmutableList()));
        }
      }
    }
    throw new IllegalArgumentException();
  }
  //
  //  @Override
  public ImmutableMap<Inputs, ValueOrError<Integer>> executeCompute(
      ImmutableList<Inputs> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                Function.identity(),
                iv -> {
                  DependencyResponse<AdderRequest, Integer> sumResponses =
                      new DependencyResponse<>(
                          iv.<Integer>getDepValue("sum").values().entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> AdderRequest.from(e.getKey()), Entry::getValue)));
                  DependencyResponse<DividerRequest, Integer> quotientResponse =
                      new DependencyResponse<>(
                          iv.<Integer>getDepValue("quotient").values().entrySet().stream()
                              .collect(
                                  toImmutableMap(
                                      e -> DividerRequest.from(e.getKey()), Entry::getValue)));
                  return ValueOrError.valueOrError(
                      () ->
                          Formula.result(
                              new FormulaAllInputs(
                                  iv.getInputValueOrThrow("a"),
                                  iv.getInputValueOrThrow("p"),
                                  iv.getInputValueOrThrow("q"),
                                  sumResponses,
                                  quotientResponse)));
                }));
  }
}

package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

public final class SplitAdderImpl123 {

  //  @Override
  //  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
  //    return ImmutableList.of(
  //        Input.builder().name("numbers").isMandatory().type(list(integer())).build(),
  //        Dependency.builder()
  //            .name("split_sum_1")
  //            .dataAccessSpec(VajramID.vajramID("splitAdder"))
  //            .build(),
  //        Dependency.builder()
  //            .name("split_sum_2")
  //            .dataAccessSpec(VajramID.vajramID("splitAdder"))
  //            .build(),
  //        Dependency.builder().name("sum").dataAccessSpec(VajramID.vajramID("adder")).build());
  //  }
  //
  //  @Override
  //  public DependencyCommand<Inputs> resolveInputOfDependency(
  //      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
  //    switch (dependency) {
  //      case "split_sum_1" -> {
  //        if (Set.of("numbers").equals(resolvableInputs)) {
  //          ArrayList<Integer> numbers = inputs.getInputValueOrThrow("numbers");
  //          DependencyCommand<ArrayList<Integer>> depCommand = numbersForSubSplitter1(numbers);
  //          if (depCommand instanceof DependencyCommand.Skip<ArrayList<Integer>> skip) {
  //            return skip.cast();
  //          } else {
  //            return multiExecuteWith(
  //                depCommand.inputs().stream()
  //                    .map(
  //                        integers ->
  //                            new Inputs(
  //                                ImmutableMap.of("numbers", ValueOrError.withValue(integers))))
  //                    .toList());
  //          }
  //        }
  //      }
  //      case "split_sum_2" -> {
  //        if (Set.of("numbers").equals(resolvableInputs)) {
  //          ArrayList<Integer> numbers = inputs.getInputValueOrThrow("numbers");
  //          DependencyCommand<ArrayList<Integer>> depCommand = numbersForSubSplitter2(numbers);
  //          if (depCommand instanceof DependencyCommand.Skip<ArrayList<Integer>> skip) {
  //            return skip.cast();
  //          } else {
  //            return multiExecuteWith(
  //                depCommand.inputs().stream()
  //                    .map(
  //                        integers ->
  //                            new Inputs(
  //                                ImmutableMap.of("numbers", ValueOrError.withValue(integers))))
  //                    .toList());
  //          }
  //        }
  //      }
  //      case "sum" -> {
  //        if (Set.of("number_one").equals(resolvableInputs)) {
  //          ArrayList<Integer> numbers = inputs.getInputValueOrThrow("numbers");
  //          DependencyCommand<Integer> depCommand = adderNumberOne(numbers);
  //          if (depCommand instanceof DependencyCommand.Skip<Integer> skip) {
  //            return skip.cast();
  //          } else {
  //            return multiExecuteWith(
  //                depCommand.inputs().stream()
  //                    .map(
  //                        integer ->
  //                            new Inputs(
  //                                ImmutableMap.of("number_one", ValueOrError.withValue(integer))))
  //                    .toList());
  //          }
  //        }
  //        if (Set.of("number_two").equals(resolvableInputs)) {
  //          ArrayList<Integer> numbers = inputs.getInputValueOrThrow("numbers");
  //          Integer value = adderNumberTwo(numbers);
  //          DependencyCommand<Integer> depCommand =
  //              Optional.ofNullable(value)
  //                  .map(DependencyCommand::executeWith)
  //                  .orElse(DependencyCommand.executeWith(null));
  //          return multiExecuteWith(
  //              depCommand.inputs().stream()
  //                  .map(
  //                      integer ->
  //                          new Inputs(
  //                              ImmutableMap.of("number_two", ValueOrError.withValue(integer))))
  //                  .toList());
  //        }
  //      }
  //    }
  //    throw new UnsupportedOperationException();
  //  }
  //
  //  @Override
  //  public ImmutableMap<Inputs, ValueOrError<Integer>> executeCompute(
  //      ImmutableList<Inputs> inputsList) {
  //    return inputsList.stream()
  //        .collect(
  //            toImmutableMap(
  //                Function.identity(),
  //                inputValues -> {
  //                  ArrayList<Integer> numbers = inputValues.getInputValueOrThrow("numbers");
  //                  Map<Inputs, ValueOrError<Integer>> splitSum1Result =
  //                      inputValues.<Integer>getDepValue("split_sum_1").values();
  //                  DependencyResponse<SplitAdderRequest, Integer> splitSum1 =
  //                      new DependencyResponse<>(
  //                          splitSum1Result.entrySet().stream()
  //                              .collect(
  //                                  toImmutableMap(
  //                                      e -> SplitAdderRequest.from(e.getKey()),
  // Entry::getValue)));
  //                  Map<Inputs, ValueOrError<Integer>> splitSum2Result =
  //                      inputValues.<Integer>getDepValue("split_sum_2").values();
  //                  DependencyResponse<SplitAdderRequest, Integer> splitSum2 =
  //                      new DependencyResponse<>(
  //                          splitSum2Result.entrySet().stream()
  //                              .collect(
  //                                  toImmutableMap(
  //                                      e -> SplitAdderRequest.from(e.getKey()),
  // Entry::getValue)));
  //                  Map<Inputs, ValueOrError<Integer>> sumResult =
  //                      inputValues.<Integer>getDepValue("sum").values();
  //                  DependencyResponse<AdderRequest, Integer> sum =
  //                      new DependencyResponse<>(
  //                          sumResult.entrySet().stream()
  //                              .collect(
  //                                  toImmutableMap(
  //                                      e -> AdderRequest.from(e.getKey()), Entry::getValue)));
  //                  return valueOrError(() -> add(new SplitAdderAllInputs(numbers, splitSum1,
  // splitSum2, sum)));
  //                }));
  //  }
}

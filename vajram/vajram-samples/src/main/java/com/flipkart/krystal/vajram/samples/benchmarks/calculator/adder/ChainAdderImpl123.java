package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

public final class ChainAdderImpl123 {

  //  @Override
  //  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
  //    return ImmutableList.of(
  //        Input.builder().name("numbers").isMandatory().type(list(integer())).build(),
  //        Dependency.builder()
  //            .name("chain_sum")
  //            .dataAccessSpec(VajramID.vajramID("chainAdder"))
  //            .build(),
  //        Dependency.builder().name("sum").dataAccessSpec(VajramID.vajramID("adder")).build());
  //  }
  //
  //  @Override
  //  public DependencyCommand<Inputs> resolveInputOfDependency(
  //      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
  //    switch (dependency) {
  //      case "chain_sum" -> {
  //        if (Set.of("numbers").equals(resolvableInputs)) {
  //          ArrayList<Integer> numbers = inputs.getInputValueOrThrow("numbers");
  //          DependencyCommand<List<Integer>> depCommand = numbersForSubChainer(numbers);
  //          if (depCommand instanceof DependencyCommand.Skip<List<Integer>> skip) {
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
  //          List<Integer> numbers = inputs.getInputValueOrThrow("numbers");
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
  //          DependencyCommand<Integer> depCommand =
  //              Optional.ofNullable(adderNumberTwo(numbers))
  //                  .orElse(DependencyCommand.executeWith(null));
  //          if (depCommand instanceof DependencyCommand.Skip<Integer> skip) {
  //            return skip.cast();
  //          } else {
  //            return multiExecuteWith(
  //                depCommand.inputs().stream()
  //                    .map(
  //                        integer ->
  //                            new Inputs(
  //                                ImmutableMap.of("number_two", ValueOrError.withValue(integer))))
  //                    .toList());
  //          }
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
  //                identity(),
  //                inputValues -> {
  //                  ArrayList<Integer> numbers = inputValues.getInputValueOrThrow("numbers");
  //                  Map<Inputs, ValueOrError<Integer>> chainSumResult =
  //                      inputValues.<Integer>getDepValue("chain_sum").values();
  //                  DependencyResponse<ChainAdderRequest, Integer> chainSum =
  //                      new DependencyResponse<>(
  //                          chainSumResult.entrySet().stream()
  //                              .collect(
  //                                  toImmutableMap(
  //                                      e -> ChainAdderRequest.from(e.getKey()),
  // Entry::getValue)));
  //                  Map<Inputs, ValueOrError<Integer>> sumResult =
  //                      inputValues.<Integer>getDepValue("sum").values();
  //                  DependencyResponse<AdderRequest, Integer> sum =
  //                      new DependencyResponse<>(
  //                          sumResult.entrySet().stream()
  //                              .collect(
  //                                  toImmutableMap(
  //                                      e -> AdderRequest.from(e.getKey()), Entry::getValue)));
  //                    return valueOrError(() -> add(new ChainAdderAllInputs(numbers, chainSum,
  // sum)));
  //                }));
  //  }
}

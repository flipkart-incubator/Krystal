package com.flipkart.krystal.vajram.samples.benchmarks.calculator.multiplier;

public final class MultiplierImpl123 {
  //  @Override
  //  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
  //    return ImmutableList.of(
  //        Input.builder().name("number_one").type(IntegerType.integer()).isMandatory().build(),
  //        Input.builder().name("number_two").type(IntegerType.integer()).isMandatory().build());
  //  }
  //
  //  @Override
  //  public ImmutableMap<Inputs, ValueOrError<Integer>> executeCompute(
  //      ImmutableList<Inputs> inputsList) {
  //    ImmutableMap.Builder<Inputs, ValueOrError<Integer>> result =
  //        ImmutableMap.builderWithExpectedSize(inputsList.size());
  //    for (Inputs inputs : inputsList) {
  //      result.put(
  //          inputs,
  //          valueOrError(
  //              () ->
  //                  multiply(
  //                      new MultiplierAllInputs(
  //                          inputs.getInputValueOrThrow("number_one"),
  //                          inputs.getInputValueOrDefault("number_two", null)))));
  //    }
  //    return result.build();
  //  }
}

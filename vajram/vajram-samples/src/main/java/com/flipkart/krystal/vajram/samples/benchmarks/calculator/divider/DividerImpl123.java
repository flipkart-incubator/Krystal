package com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider;

public final class DividerImpl123 {
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
  //    Map<Inputs, ValueOrError<Integer>> result = new HashMap<>(inputsList.size());
  //    for (Inputs inputs : inputsList) {
  //      result.put(
  //          inputs,
  //          ValueOrError.valueOrError(
  //              () ->
  //                  divide(
  //                      new DividerAllInputs(
  //                          inputs.getInputValueOrThrow("number_one"),
  //                          inputs.getInputValueOrDefault("number_two", null)))));
  //    }
  //    return ImmutableMap.copyOf(result);
  //  }
}

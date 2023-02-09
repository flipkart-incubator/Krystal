package com.flipkart.krystal.vajram.samples.benchmarks.calculator.multiplier;

import static com.flipkart.krystal.data.ValueOrError.valueOrError;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.datatypes.IntegerType;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.multiplier.MultiplierInputUtil.AllInputs;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public final class MultiplierImpl extends Multiplier {
  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("number_one").type(IntegerType.integer()).isMandatory().build(),
        Input.builder().name("number_two").type(IntegerType.integer()).isMandatory().build());
  }

  @Override
  public ImmutableMap<Inputs, ValueOrError<Integer>> executeCompute(
      ImmutableList<Inputs> inputsList) {
    ImmutableMap.Builder<Inputs, ValueOrError<Integer>> result =
        ImmutableMap.builderWithExpectedSize(inputsList.size());
    for (Inputs inputs : inputsList) {
      result.put(
          inputs,
          valueOrError(
              () ->
                  multiply(
                      new AllInputs(
                          inputs.getInputValueOrThrow("number_one"),
                          inputs.getInputValueOrDefault("number_two", null)))));
    }
    return result.build();
  }
}

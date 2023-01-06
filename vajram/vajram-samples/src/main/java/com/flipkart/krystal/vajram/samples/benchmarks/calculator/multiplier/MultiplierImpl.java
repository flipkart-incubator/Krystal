package com.flipkart.krystal.vajram.samples.benchmarks.calculator.multiplier;

import com.flipkart.krystal.datatypes.IntegerType;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public class MultiplierImpl extends Multiplier {
  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("number_one").type(IntegerType.integer()).isMandatory().build(),
        Input.builder().name("number_two").type(IntegerType.integer()).isMandatory().build());
  }

  @Override
  public ImmutableMap<InputValues, ImmutableList<Integer>> executeCompute(
      ImmutableList<InputValues> inputsList) {
    Map<InputValues, ImmutableList<Integer>> result = new HashMap<>();
    for (InputValues inputValues : inputsList) {
      result.put(
          inputValues,
          ImmutableList.of(
              multiply(
                  new MultiplierInputUtil.AllInputs(
                      inputValues.getOrThrow("number_one"),
                      inputValues.getOrDefault("number_two", null)))));
    }
    return ImmutableMap.copyOf(result);
  }
}

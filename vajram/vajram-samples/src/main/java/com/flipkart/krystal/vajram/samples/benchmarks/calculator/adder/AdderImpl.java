package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import com.flipkart.krystal.datatypes.IntegerType;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderInputUtil.AllInputs;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public class AdderImpl extends Adder {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("number_one").type(IntegerType.integer()).isMandatory().build(),
        Input.builder().name("number_two").type(IntegerType.integer()).build());
  }

  @Override
  public ImmutableMap<Inputs, Integer> executeCompute(ImmutableList<Inputs> inputsList) {
    Map<Inputs, Integer> result = new HashMap<>();
    for (Inputs inputs : inputsList) {
      result.put(
          inputs,
          add(
              new AllInputs(
                  inputs.getInputValueOrThrow("number_one"),
                  inputs.getInputValueOrDefault("number_two", null))));
    }
    return ImmutableMap.copyOf(result);
  }
}

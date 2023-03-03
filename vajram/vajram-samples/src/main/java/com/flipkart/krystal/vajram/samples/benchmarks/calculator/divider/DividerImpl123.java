package com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.datatypes.IntegerType;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerInputUtil.DividerAllInputs;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public final class DividerImpl123  {
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

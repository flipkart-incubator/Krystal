package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderInputUtil.AdderCommonInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderInputUtil.AdderModInputs;
import java.util.Map;

@VajramDef("adder")
public abstract class Adder extends ComputeVajram<Integer> {
  @VajramLogic
  public static Map<AdderModInputs, Integer> add(
      ModulatedInput<AdderModInputs, AdderCommonInputs> modulatedInput) {
    return modulatedInput.inputsNeedingModulation().stream()
        .collect(toImmutableMap(identity(), im -> add(im.numberOne(), im.numberTwo().orElse(0))));
  }

  public static int add(int a, int b) {
    return a + b;
  }
}

package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder.ID;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderInputUtil.AdderCommonInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderInputUtil.AdderModInputs;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

@VajramDef(ID)
public abstract class Adder extends ComputeVajram<Integer> {

  public static final String ID = "adder";
  public static final LongAdder CALL_COUNTER = new LongAdder();

  @VajramLogic
  public static Map<AdderModInputs, Integer> add(
      ModulatedInput<AdderModInputs, AdderCommonInputs> modulatedInput) {
    CALL_COUNTER.increment();
    return modulatedInput.modInputs().stream()
        .collect(toImmutableMap(identity(), im -> add(im.numberOne(), im.numberTwo().orElse(0))));
  }

  public static int add(int a, int b) {
    return a + b;
  }
}

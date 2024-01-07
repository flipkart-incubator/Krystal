package com.flipkart.krystal.vajram.samples.calculator.adder;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.modulation.Modulated;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.samples.calculator.adder.AdderInputUtil.AdderCommonInputs;
import com.flipkart.krystal.vajram.samples.calculator.adder.AdderInputUtil.AdderModInputs;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Adder extends ComputeVajram<Integer> {

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Modulated @Input int numberOne;

  @Modulated @Input Optional<Integer> numberTwo;

  @VajramLogic
  static Map<AdderModInputs, Integer> add(
      ModulatedInput<AdderModInputs, AdderCommonInputs> modulatedInput) {
    CALL_COUNTER.increment();
    return modulatedInput.modInputs().stream()
        .collect(toImmutableMap(identity(), im -> add(im.numberOne(), im.numberTwo().orElse(0))));
  }

  public static int add(int a, int b) {
    return a + b;
  }
}

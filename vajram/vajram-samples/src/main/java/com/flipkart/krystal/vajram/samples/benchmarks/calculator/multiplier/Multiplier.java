package com.flipkart.krystal.vajram.samples.benchmarks.calculator.multiplier;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.multiplier.MultiplierInputUtil.MultiplierInputs;

@VajramDef("multiplier")
public abstract class Multiplier extends ComputeVajram<Integer> {
  @VajramLogic
  public static int multiply(MultiplierInputs allInputs) {
    return allInputs.numberOne() * allInputs.numberTwo().orElse(1);
  }
}

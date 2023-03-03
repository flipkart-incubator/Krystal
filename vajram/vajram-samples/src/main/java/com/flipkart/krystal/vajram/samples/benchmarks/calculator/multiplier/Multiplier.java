package com.flipkart.krystal.vajram.samples.benchmarks.calculator.multiplier;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.multiplier.MultiplierInputUtil.MultiplierAllInputs;

@VajramDef("multiplier")
public abstract class Multiplier extends ComputeVajram<Integer> {
  @VajramLogic
  public int multiply(MultiplierAllInputs allInputs) {
    return allInputs.numberOne() * allInputs.numberTwo().orElse(1);
  }
}

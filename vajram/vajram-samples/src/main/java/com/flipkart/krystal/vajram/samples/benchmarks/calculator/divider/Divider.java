package com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerInputUtil.DividerAllInputs;

@VajramDef("divider")
public abstract class Divider extends ComputeVajram<Integer> {
  @VajramLogic
  public static int divide(DividerAllInputs allInputs) {
    return divide(allInputs.numerator(), allInputs.denominator().orElse(1));
  }

  public static int divide(int a, int b) {
    return a / b;
  }
}

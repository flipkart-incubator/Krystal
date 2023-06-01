package com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerInputUtil.DividerAllInputs;

@VajramDef("divider")
public abstract class Divider extends ComputeVajram<Integer> {
  @VajramLogic
  public static int divide(DividerAllInputs DividerAllInputs) {
    return divide(DividerAllInputs.numberOne(), DividerAllInputs.numberTwo().orElse(1));
  }

  public static int divide(int a, int b) {
    return a / b;
  }
}

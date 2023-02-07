package com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerInputUtil.AllInputs;

@VajramDef("divider")
public abstract class Divider extends ComputeVajram<Integer> {
  @VajramLogic
  public int divide(AllInputs allInputs) {
    return divide(allInputs.numberOne(), allInputs.numberTwo().orElse(1));
  }

  public int divide(int a, int b) {
    return a / b;
  }
}

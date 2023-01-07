package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderInputUtil.AllInputs;

@VajramDef("adder")
public abstract class Adder extends ComputeVajram<Integer> {
  @VajramLogic
  public static int add(AllInputs allInputs) {
    return add(allInputs.numberOne(), allInputs.numberTwo().orElse(0));
  }

  public static int add(int a, int b) {
    return a + b;
  }
}

package com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerInputUtil.DividerInputs;
import java.util.Optional;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Divider extends ComputeVajram<Integer> {
  @Input int numerator;
  @Input Optional<Integer> denominator;

  @VajramLogic
  static int divide(DividerInputs allInputs) {
    return divide(allInputs.numerator(), allInputs.denominator().orElse(1));
  }

  public static int divide(int a, int b) {
    return a / b;
  }
}

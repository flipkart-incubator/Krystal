package com.flipkart.krystal.vajram.samples.calculator.divide;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;

@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
@Vajram
public abstract class Divide extends ComputeVajramDef<Integer> {
  static class _Inputs {

    @IfAbsent(FAIL)
    int numerator;

    int denominator;
  }

  @Output
  static int divide(int numerator, Optional<Integer> denominator) {
    return divide(numerator, denominator.orElse(1));
  }

  public static int divide(int numerator, int denominator) {
    return numerator / denominator;
  }
}

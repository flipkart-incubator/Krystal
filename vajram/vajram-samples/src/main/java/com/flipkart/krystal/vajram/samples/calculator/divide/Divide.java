package com.flipkart.krystal.vajram.samples.calculator.divide;

import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;

@Vajram
@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
public abstract class Divide extends ComputeVajramDef<Integer> {
  static class _Facets {
    @IfNoValue
    @Input int numerator;
    @Input int denominator;
  }

  @Output
  static int divide(int numerator, Optional<Integer> denominator) {
    return divide(numerator, denominator.orElse(1));
  }

  public static int divide(int numerator, int denominator) {
    return numerator / denominator;
  }
}

package com.flipkart.krystal.vajram.samples.calculator.divider;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.VajramDef;
import java.util.Optional;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Divider extends ComputeVajram<Integer> {
  static class _Facets {
    @Input int numerator;
    @Input Optional<Integer> denominator;
  }

  @Output
  static int divide(DividerFacets facets) {
    return divide(facets.numerator(), facets.denominator().orElse(1));
  }

  public static int divide(int a, int b) {
    return a / b;
  }
}

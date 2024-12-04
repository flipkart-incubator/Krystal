package com.flipkart.krystal.vajram.samples.calculator.divider;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.samples.calculator.divider.DividerFacetUtil.DividerFacets;
import java.util.Optional;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Divider extends ComputeVajram<Integer> {
  static class _Facets {
    @Input int numerator;
    @Input Optional<Integer> denominator;
  }

  @Output
  static Integer divide(DividerFacets facets) {
    return facets.numerator() / facets.denominator().orElse(1);
  }

  public static int divide(int numerator, int denominator) {
    return numerator / denominator;
  }
}

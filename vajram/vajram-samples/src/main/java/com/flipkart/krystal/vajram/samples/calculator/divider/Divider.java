package com.flipkart.krystal.vajram.samples.calculator.divider;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.samples.calculator.divider.DividerFacetUtil.DividerFacets;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Divider extends ComputeVajram<Integer> {
  static class _Facets {
    @Input int numerator;
    @Input Optional<Integer> denominator;
  }

  @Output
  static @Nullable Integer divide(DividerFacets facets) {
    return divide(facets.numerator(), facets.denominator().orElse(1));
  }

  public static @Nullable Integer divide(int a, int b) {
    if (b == 0) {
      return null;
    }
    return a / b;
  }
}

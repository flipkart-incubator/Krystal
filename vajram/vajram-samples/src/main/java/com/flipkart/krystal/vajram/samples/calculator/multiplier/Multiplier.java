package com.flipkart.krystal.vajram.samples.calculator.multiplier;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.samples.calculator.multiplier.MultiplierFacetUtil.MultiplierFacets;
import java.util.Optional;

@ExternalInvocation(allow = true)
@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Multiplier extends ComputeVajram<Integer> {
  static class _Facets {
    @Input int numberOne;
    @Input Optional<Integer> numberTwo;
  }

  @Output
  static int multiply(MultiplierFacets allInputs) {
    return allInputs.numberOne() * allInputs.numberTwo().orElse(1);
  }
}

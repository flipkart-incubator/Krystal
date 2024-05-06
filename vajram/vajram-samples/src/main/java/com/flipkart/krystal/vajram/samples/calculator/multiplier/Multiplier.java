package com.flipkart.krystal.vajram.samples.calculator.multiplier;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.VajramDef;
import java.util.Optional;

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

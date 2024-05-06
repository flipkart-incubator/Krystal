package com.flipkart.krystal.vajram.samples.calculator.subtractor;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.VajramDef;
import java.util.Optional;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Subtractor extends ComputeVajram<Integer> {
  static class _Facets {
    @Input int numberOne;
    @Input Optional<Integer> numberTwo;
  }

  @Output
  static int subtract(SubtractorFacets allInputs) {
    return allInputs.numberOne() - allInputs.numberTwo().orElse(0);
  }
}

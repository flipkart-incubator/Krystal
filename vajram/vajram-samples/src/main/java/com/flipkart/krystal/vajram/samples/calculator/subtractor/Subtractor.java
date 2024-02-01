package com.flipkart.krystal.vajram.samples.calculator.subtractor;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.samples.calculator.subtractor.SubtractorFacetUtil.SubtractorFacets;
import java.util.Optional;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Subtractor extends ComputeVajram<Integer> {
  @Input int numberOne;
  @Input Optional<Integer> numberTwo;

  @Output
  public static int subtract(SubtractorFacets allInputs) {
    return allInputs.numberOne() - allInputs.numberTwo().orElse(0);
  }
}

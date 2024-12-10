package com.flipkart.krystal.vajram.samples.calculator.subtractor;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;

@ExternalInvocation(allow = true)
@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Subtractor extends ComputeVajram<Integer> {
  static class _Facets {
    @Input int numberOne;
    @Input Optional<Integer> numberTwo;
  }

  @Output
  static int subtract(SubtractorFacets _allFacets) {
    return _allFacets.numberOne() - _allFacets.numberTwo().orElse(0);
  }
}

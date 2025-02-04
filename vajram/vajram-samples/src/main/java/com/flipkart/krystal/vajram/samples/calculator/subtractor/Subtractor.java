package com.flipkart.krystal.vajram.samples.calculator.subtractor;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;

@ExternalInvocation(allow = true)
@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Subtractor extends ComputeVajram<Integer> {
  static class _Facets {
    @Mandatory @Input int numberOne;
    @Input int numberTwo;
  }

  @Output
  static int subtract(int numberOne, Optional<Integer> numberTwo) {
    return numberOne - numberTwo.orElse(0);
  }
}

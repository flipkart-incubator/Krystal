package com.flipkart.krystal.vajram.samples.calculator.subtract;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;

@ExternalInvocation(allow = true)
@Vajram
@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
public abstract class Subtract extends ComputeVajramDef<Integer> {
  static class _Facets {
    @Mandatory @Input int numberOne;
    @Input int numberTwo;
  }

  @Output
  static int subtract(int numberOne, Optional<Integer> numberTwo) {
    return numberOne - numberTwo.orElse(0);
  }
}

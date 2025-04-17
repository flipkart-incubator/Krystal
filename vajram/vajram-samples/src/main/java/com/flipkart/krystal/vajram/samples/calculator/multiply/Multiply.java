package com.flipkart.krystal.vajram.samples.calculator.multiply;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;

@ExternallyInvocable
@Vajram
@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
public abstract class Multiply extends ComputeVajramDef<Integer> {
  static class _Inputs {
    @IfNull(FAIL)
    int numberOne;

    int numberTwo;
  }

  @Output
  static int multiply(int numberOne, Optional<Integer> numberTwo) {
    return numberOne * numberTwo.orElse(1);
  }
}

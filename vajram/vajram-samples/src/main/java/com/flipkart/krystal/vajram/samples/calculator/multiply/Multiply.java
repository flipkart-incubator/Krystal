package com.flipkart.krystal.vajram.samples.calculator.multiply;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;

@InvocableOutsideGraph
@Vajram
@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
public abstract class Multiply extends ComputeVajramDef<Integer> {
  static class _Inputs {
    @IfAbsent(FAIL)
    int numberOne;

    int numberTwo;
  }

  @Output
  static int multiply(int numberOne, Optional<Integer> numberTwo) {
    return numberOne * numberTwo.orElse(1);
  }
}

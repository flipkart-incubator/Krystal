package com.flipkart.krystal.vajram.samples.calculator.subtract;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;

@InvocableOutsideGraph
@Vajram
@SuppressWarnings("optional.parameter")
public abstract class Subtract extends ComputeVajramDef<Integer> {
  interface _Inputs {
    @IfAbsent(FAIL)
    int numberOne();

    int numberTwo();
  }

  @Output
  static int subtract(int numberOne, Optional<Integer> numberTwo) {
    return numberOne - numberTwo.orElse(0);
  }
}

package com.flipkart.krystal.vajram.samples.calculator.subtract;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.util.Objects.requireNonNullElse;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import org.checkerframework.checker.nullness.qual.Nullable;

@InvocableOutsideGraph
@Vajram
public abstract class Subtract extends ComputeVajramDef<Integer> {
  interface _Inputs {
    @IfAbsent(FAIL)
    int numberOne();

    int numberTwo();
  }

  @Output
  static int subtract(int numberOne, @Nullable Integer numberTwo) {
    return numberOne - requireNonNullElse(numberTwo, 0);
  }
}

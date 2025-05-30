package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.List;

/** Adds all the {@code numbers} and returns the result */
@Vajram
public abstract class SimpleAdd extends ComputeVajramDef<Integer> implements MultiAdd {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(ASSUME_DEFAULT_VALUE)
    List<Integer> numbers;
  }

  @Output
  public static int output(List<Integer> numbers) {
    return numbers.stream().mapToInt(i -> i).sum();
  }
}

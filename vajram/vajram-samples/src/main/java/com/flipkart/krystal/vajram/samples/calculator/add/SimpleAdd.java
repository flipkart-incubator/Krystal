package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.data.IfNull.IfNullThen.DEFAULT_TO_EMPTY;

import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.List;

/** Adds all the {@code numbers} and returns the result */
@Vajram
public abstract class SimpleAdd extends ComputeVajramDef<Integer> implements MultiAdd {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfNull(DEFAULT_TO_EMPTY)
    List<Integer> numbers;
  }

  @Output
  public static int output(List<Integer> numbers) {
    return numbers.stream().mapToInt(i -> i).sum();
  }
}

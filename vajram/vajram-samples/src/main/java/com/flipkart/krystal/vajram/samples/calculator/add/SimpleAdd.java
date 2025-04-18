package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.data.IfNull.IfNullThen.DEFAULT_TO_EMPTY;
import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;

import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.annos.ConformsToTrait;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.List;

/** Adds all the {@code numbers} and returns the result */
@Vajram
@ConformsToTrait(withDef = MultiAdd.class)
public abstract class SimpleAdd extends ComputeVajramDef<Integer> {
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

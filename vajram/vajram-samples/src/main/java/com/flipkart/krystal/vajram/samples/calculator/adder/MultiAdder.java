package com.flipkart.krystal.vajram.samples.calculator.adder;

import com.flipkart.krystal.vajram.VajramTrait;
import com.flipkart.krystal.vajram.VajramTraitDef;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import java.util.List;

/** Adds all the {@code numbers} and returns the result */
@VajramTrait
public class MultiAdder implements VajramTraitDef<Integer> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @Mandatory @Input List<Integer> numbers;
  }
}

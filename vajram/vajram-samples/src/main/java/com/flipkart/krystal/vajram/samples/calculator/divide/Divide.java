package com.flipkart.krystal.vajram.samples.calculator.divide;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
@Vajram
public abstract class Divide extends IOVajramDef<Integer> {

  static class _Inputs {

    @IfAbsent(FAIL)
    int numerator;

    int denominator;
  }

  @Output
  static CompletableFuture<Integer> divide(int numerator, Optional<Integer> denominator) {
    return supplyAsync(() -> divide(numerator, denominator.orElse(1)));
  }

  public static int divide(int numerator, int denominator) {
    return numerator / denominator;
  }
}

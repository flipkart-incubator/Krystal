package com.flipkart.krystal.vajram.samples.calculator.adder;

import static com.flipkart.krystal.vajram.facets.Mandatory.IfNotSet.DEFAULT_TO_ZERO;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

@VajramDef
public abstract class Adder extends IOVajram<Integer> {
  public static final LongAdder CALL_COUNTER = new LongAdder();
  public static final String FAIL_ADDER_FLAG = "failAdder";

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Facets {
    @Mandatory @Batch @Input int numberOne;

    @Mandatory(ifNotSet = DEFAULT_TO_ZERO)
    @Batch
    @Input
    int numberTwo;

    @Inject
    @Named(FAIL_ADDER_FLAG)
    boolean fail;
  }

  @Output
  static Map<Adder_BatchElem, CompletableFuture<Integer>> add(
      ImmutableList<Adder_BatchElem> _batches, Optional<Boolean> fail) {
    CALL_COUNTER.increment();
    if (fail.orElse(false)) {
      throw new RuntimeException("Adder failed because fail flag was set");
    }
    return _batches.stream()
        .collect(
            toImmutableMap(
                identity(), batch -> completedFuture(add(batch.numberOne(), batch.numberTwo()))));
  }

  public static int add(int a, int b) {
    return a + b;
  }
}

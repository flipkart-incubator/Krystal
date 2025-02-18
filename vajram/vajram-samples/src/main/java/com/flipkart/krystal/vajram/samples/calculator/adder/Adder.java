package com.flipkart.krystal.vajram.samples.calculator.adder;

import static com.flipkart.krystal.vajram.facets.Mandatory.IfNotSet.DEFAULT_TO_ZERO;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.google.common.collect.ImmutableCollection;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

@VajramDef
@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
public abstract class Adder extends IOVajram<Integer> {
  static class _Facets {
    @Mandatory @Batched @Input int numberOne;

    @Mandatory(ifNotSet = DEFAULT_TO_ZERO)
    @Batched
    @Input
    int numberTwo;

    @BatchesGroupedBy
    @Named(FAIL_ADDER_FLAG)
    @Inject
    boolean fail;
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();

  public static final String FAIL_ADDER_FLAG = "failAdder";

  @Output
  static Map<Adder_BatchItem, CompletableFuture<Integer>> add(
      ImmutableCollection<Adder_BatchItem> _batchItems, Optional<Boolean> fail) {
    CALL_COUNTER.increment();
    if (fail.orElse(false)) {
      throw new RuntimeException("Adder failed because fail flag was set");
    }
    return _batchItems.stream()
        .collect(
            toImmutableMap(
                identity(), batch -> completedFuture(add(batch.numberOne(), batch.numberTwo()))));
  }

  public static int add(int a, int b) {
    return a + b;
  }
}

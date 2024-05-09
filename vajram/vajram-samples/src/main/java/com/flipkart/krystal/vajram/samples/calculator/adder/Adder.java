package com.flipkart.krystal.vajram.samples.calculator.adder;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.samples.calculator.adder.AdderFacetUtil.AdderCommonFacets;
import com.flipkart.krystal.vajram.samples.calculator.adder.AdderFacetUtil.AdderInputBatch;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

@VajramDef
@SuppressWarnings("initialization.field.uninitialized")
public abstract class Adder extends ComputeVajram<Integer> {
  static class _Facets {
    @Batch @Input int numberOne;
    @Batch @Input Optional<Integer> numberTwo;
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Output
  static Map<AdderInputBatch, Integer> add(
      BatchedFacets<AdderInputBatch, AdderCommonFacets> batchedFacets) {
    CALL_COUNTER.increment();
    return batchedFacets.batchedInputs().stream()
        .collect(toImmutableMap(identity(), im -> add(im.numberOne(), im.numberTwo().orElse(0))));
  }

  public static int add(int a, int b) {
    return a + b;
  }
}

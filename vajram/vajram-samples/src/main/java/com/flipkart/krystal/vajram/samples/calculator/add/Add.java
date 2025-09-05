package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.NonNil;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
import com.flipkart.krystal.vajram.facets.Output;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

/**
 * Adds two numbers - {@code numberOne} and {@code numberTwo} and returns the result. While {@code
 * numberOne} is a mandatory input, {@code numberTwo} is optional and defaults to zero if not set.
 */
@SuppressWarnings({"initialization.field.uninitialized", "optional.parameter"})
@Vajram
public abstract class Add extends IOVajramDef<Integer> {

  static class _Inputs {
    /** The first number to add */
    @IfAbsent(FAIL)
    @Batched
    int numberOne;

    /** The second number to add */
    @IfAbsent(ASSUME_DEFAULT_VALUE)
    @Batched
    int numberTwo;
  }

  static class _InternalFacets {
    /**
     * Flag to indicate if the adder should fail. If set to true, the adder will throw a {@link
     * RuntimeException}
     */
    @BatchesGroupedBy
    @Named(FAIL_ADDER_FLAG)
    @Inject
    boolean fail;
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();

  public static final String FAIL_ADDER_FLAG = "failAdder";

  @Output.Batched
  static CompletableFuture<BatchAddResult> batchedOutput(
      ImmutableCollection<Add_BatchItem> _batchItems, Optional<Boolean> fail) {
    CALL_COUNTER.increment();
    if (fail.orElse(false)) {
      throw new RuntimeException("Adder failed because fail flag was set");
    }
    return completedFuture(
        new BatchAddResult(
            _batchItems.stream()
                .collect(
                    toImmutableMap(
                        addBatchItem ->
                            ImmutableList.of(addBatchItem.numberOne(), addBatchItem.numberTwo()),
                        batch -> add(batch.numberOne(), batch.numberTwo())))));
  }

  @Output.Unbatch
  static Map<Add_BatchItem, Errable<Integer>> unbatchOutput(
      Errable<BatchAddResult> _batchedOutput) {
    if (!(_batchedOutput instanceof NonNil<BatchAddResult> nonNil)) {
      return ImmutableMap.of();
    }
    return nonNil.value().result().entrySet().stream()
        .collect(
            toImmutableMap(
                entry ->
                    Add_BatchItem._pojoBuilder()
                        .numberOne(entry.getKey().get(0))
                        .numberTwo(entry.getKey().get(1))
                        ._build(),
                entry -> Errable.withValue(entry.getValue())));
  }

  public static int add(int a, int b) {
    return a + b;
  }

  record BatchAddResult(ImmutableMap<ImmutableList<Integer>, Integer> result) {}
}

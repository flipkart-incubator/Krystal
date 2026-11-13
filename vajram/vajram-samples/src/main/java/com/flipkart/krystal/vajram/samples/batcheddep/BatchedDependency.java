package com.flipkart.krystal.vajram.samples.batcheddep;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.samples.batcheddep.BatchedDependency_Fac.doubled_n;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.NonNil;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.samples.calculator.multiply.Multiply;
import com.flipkart.krystal.vajram.samples.calculator.multiply.Multiply_Req;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

/**
 * Demonstrates that {@code @Batched} can be placed on a dependency. Both {@code id} and the
 * dependency {@code doubled} (which is resolved from {@code id}) are {@code @Batched} and hence are
 * read via the batch items. Returns {@code id * 2 + 1}.
 */
@Vajram
public abstract class BatchedDependency extends IOVajramDef<Integer> {

  public static final LongAdder BATCH_CALLS = new LongAdder();

  interface _Inputs {
    @IfAbsent(FAIL)
    @Batched
    int id();
  }

  interface _InternalFacets {
    @IfAbsent(FAIL)
    @Batched
    @Dependency(onVajram = Multiply.class)
    int doubled();
  }

  @Resolve(dep = doubled_n, depInputs = Multiply_Req.numberOne_n)
  static int numberOne(int id) {
    return id;
  }

  @Resolve(dep = doubled_n, depInputs = Multiply_Req.numberTwo_n)
  static int numberTwo() {
    return 2;
  }

  @Output.Batched
  static CompletableFuture<ImmutableMap<Integer, Integer>> batchedOutput(
      Collection<BatchedDependency_BatchItem> _batchItems) {
    BATCH_CALLS.increment();
    ImmutableMap.Builder<Integer, Integer> results = ImmutableMap.builder();
    for (BatchedDependency_BatchItem item : _batchItems) {
      results.put(item.id(), item.doubled().response().valueOpt().orElse(0) + 1);
    }
    return CompletableFuture.completedFuture(results.buildKeepingLast());
  }

  @Output.Unbatch
  static Map<BatchedDependency_BatchItem, Errable<Integer>> unbatch(
      Collection<BatchedDependency_BatchItem> _batchItems,
      Errable<ImmutableMap<Integer, Integer>> _batchedOutput) {
    if (!(_batchedOutput instanceof NonNil<ImmutableMap<Integer, Integer>> nonNil)) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<BatchedDependency_BatchItem, Errable<Integer>> results =
        ImmutableMap.builder();
    for (BatchedDependency_BatchItem item : _batchItems) {
      results.put(item, Errable.withValue(nonNil.value().get(item.id())));
    }
    return results.buildKeepingLast();
  }
}

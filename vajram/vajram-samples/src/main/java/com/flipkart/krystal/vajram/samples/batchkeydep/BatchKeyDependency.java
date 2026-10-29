package com.flipkart.krystal.vajram.samples.batchkeydep;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.samples.batchkeydep.BatchKeyDependency_Fac.lookup_n;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.NonNil;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
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
 * Demonstrates how the facets of a batched vajram are read:
 *
 * <ul>
 *   <li>{@code id} is {@code @Batched}, so it is read via the batch items.
 *   <li>{@code base} is an input annotated with {@code @BatchesGroupedBy}, so it is a batch key and
 *       is read individually.
 *   <li>{@code lookup} is a dependency which is resolved from {@code base}. It is not
 *       {@code @Batched}, so it is read individually. Since it depends only on the batch key it has
 *       the same value for all requests in a batch.
 * </ul>
 *
 * Returns {@code id + base + lookup}, where {@code lookup} is {@code base} (computed by {@link
 * Multiply} as {@code base * 1}).
 */
@Vajram
public abstract class BatchKeyDependency extends IOVajramDef<Integer> {

  public static final LongAdder BATCH_CALLS = new LongAdder();

  interface _Inputs {
    @IfAbsent(FAIL)
    @Batched
    int id();

    @IfAbsent(FAIL)
    @BatchesGroupedBy
    int base();
  }

  interface _InternalFacets {
    @IfAbsent(FAIL)
    @Dependency(onVajram = Multiply.class)
    int lookup();
  }

  @Resolve(dep = lookup_n, depInputs = Multiply_Req.numberOne_n)
  static int numberOne(int base) {
    return base;
  }

  @Output.Batched
  static CompletableFuture<ImmutableMap<Integer, Integer>> batchedOutput(
      Collection<BatchKeyDependency_BatchItem> _batchItems, int base, int lookup) {
    BATCH_CALLS.increment();
    ImmutableMap.Builder<Integer, Integer> results = ImmutableMap.builder();
    for (BatchKeyDependency_BatchItem item : _batchItems) {
      results.put(item.id(), item.id() + base + lookup);
    }
    return CompletableFuture.completedFuture(results.buildKeepingLast());
  }

  @Output.Unbatch
  static Map<BatchKeyDependency_BatchItem, Errable<Integer>> unbatch(
      Collection<BatchKeyDependency_BatchItem> _batchItems,
      Errable<ImmutableMap<Integer, Integer>> _batchedOutput) {
    if (!(_batchedOutput instanceof NonNil<ImmutableMap<Integer, Integer>> nonNil)) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<BatchKeyDependency_BatchItem, Errable<Integer>> results =
        ImmutableMap.builder();
    for (BatchKeyDependency_BatchItem item : _batchItems) {
      results.put(item, Errable.withValue(nonNil.value().get(item.id())));
    }
    return results.buildKeepingLast();
  }
}

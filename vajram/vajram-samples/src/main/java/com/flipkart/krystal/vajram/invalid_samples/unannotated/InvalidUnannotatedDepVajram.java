package com.flipkart.krystal.vajram.invalid_samples.unannotated;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.depInput;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolvers.resolve;
import static com.flipkart.krystal.vajram.invalid_samples.unannotated.InvalidUnannotatedDepVajram_Fac.id_s;
import static com.flipkart.krystal.vajram.invalid_samples.unannotated.InvalidUnannotatedDepVajram_Fac.lookup_s;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajram.samples.calculator.add.Add_Req;
import com.google.common.collect.ImmutableCollection;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * INVALID on purpose: the dependency {@code lookup} (not {@code @Batched}, not
 * {@code @BatchesGroupedBy}) is resolved from the {@code @Batched} input {@code id}. The edge is
 * defined via {@link #getSimpleInputResolvers()} so only the runtime validation (at graph load) can
 * catch it. Used only by runtime validation tests - do not load this package in other graphs.
 */
@Vajram
public abstract class InvalidUnannotatedDepVajram extends IOVajramDef<Integer> {

  interface _Inputs {
    @IfAbsent(FAIL)
    @Batched
    int id();
  }

  interface _InternalFacets {
    @IfAbsent(FAIL)
    @Dependency(onVajram = Add.class)
    int lookup();
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(dep(lookup_s, depInput(Add_Req.numberOne_s).usingAsIs(id_s).asResolver()));
  }

  @Output.Batched
  static CompletableFuture<Integer> batchedOutput(
      Collection<InvalidUnannotatedDepVajram_BatchItem> _batchItems, int lookup) {
    return CompletableFuture.completedFuture(lookup);
  }

  @Output.Unbatch
  static Map<InvalidUnannotatedDepVajram_BatchItem, Errable<Integer>> unbatch(
      Collection<InvalidUnannotatedDepVajram_BatchItem> _batchItems,
      Errable<Integer> _batchedOutput) {
    return Map.of();
  }
}

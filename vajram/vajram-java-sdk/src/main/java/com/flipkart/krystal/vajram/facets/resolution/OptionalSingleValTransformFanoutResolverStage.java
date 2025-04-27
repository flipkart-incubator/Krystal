package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue.SingleFacetValue;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.resolution.Transformer.One2Many;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.flipkart.krystal.vajram.facets.specs.OptionalSingleValueFacetSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Stage after specifying the source input for the fanout resolver.
 *
 * @param <S> Source Type: The DataType of the source input being used for resolution
 * @param <T> Target Type: The DataType of the dependency target input being resolved
 * @param <CV> CurrentVajram: The current vajram which is resolving the input
 * @param <DV> DependencyVajram: The vajram whose input is being resolved
 */
public final class OptionalSingleValTransformFanoutResolverStage<
    S, T, CV extends Request, DV extends Request> {
  private final InputMirrorSpec<T, DV> targetInput;
  private final OptionalSingleValueFacetSpec<S, CV> sourceFacet;
  private final List<SkipPredicate> skipConditions = new ArrayList<>();

  OptionalSingleValTransformFanoutResolverStage(
      InputMirrorSpec<T, DV> targetInput, OptionalSingleValueFacetSpec<S, CV> sourceFacet) {
    this.targetInput = targetInput;
    this.sourceFacet = sourceFacet;
  }

  /**
   * Specify the condition when the dependency needs to be skipped
   *
   * @param whenToSkip The condition when the dependency needs to be skipped
   * @param reason The reason for skipping the dependency
   */
  @SuppressWarnings("unchecked")
  public OptionalSingleValTransformFanoutResolverStage<S, T, CV, DV> skipIf(
      Predicate<Errable<S>> whenToSkip, String reason) {
    this.skipConditions.add(
        new SkipPredicate(
            reason, facetValue -> whenToSkip.test(((SingleFacetValue<S>) facetValue).asErrable())));
    return this;
  }

  /**
   * Final stage of the input resolution DSL. Specify the logic to use to transform the source input
   * into a collection of values for the fanout target dependency inputs
   *
   * @param transformer The logic to use to tranform the source data type {@code S} and fanout to a
   *     collection of the target data type {@code T}
   * @return The resultant {@link SimpleInputResolverSpec}
   */
  @SuppressWarnings("unchecked")
  public SimpleInputResolverSpec<T, CV, DV> asResolver(
      Function<Errable<S>, ? extends Collection<? extends T>> transformer) {
    return new SimpleInputResolverSpec<>(
        targetInput,
        sourceFacet,
        skipConditions,
        new One2Many(
            facetValue -> transformer.apply(((SingleFacetValue<S>) facetValue).asErrable())));
  }
}

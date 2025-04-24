package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.specs.FanoutDepSpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
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
 * @param <SDV> Source's Dependency Vajram: The vajram whose output is the source Facet.
 */
public final class MultiValTransformFanoutResolverStage<
    S, T, CV extends Request<?>, DV extends Request<?>, SDV extends Request<S>> {
  private final InputMirrorSpec<T, DV> targetInput;
  private final FanoutDepSpec<S, CV, SDV> sourceFacet;
  private final List<SkipPredicate> skipConditions = new ArrayList<>();

  MultiValTransformFanoutResolverStage(
      InputMirrorSpec<T, DV> targetInput, FanoutDepSpec<S, CV, SDV> sourceFacet) {
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
  public MultiValTransformFanoutResolverStage<S, T, CV, DV, SDV> skipIf(
      Predicate<FanoutDepResponses<?, S>> whenToSkip, String reason) {
    this.skipConditions.add(
        new SkipPredicate(
            reason, facetValue -> whenToSkip.test((FanoutDepResponses<?, S>) facetValue)));
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
      Function<FanoutDepResponses<SDV, S>, ? extends Collection<? extends T>> transformer) {
    return new SimpleInputResolverSpec<>(
        targetInput,
        sourceFacet,
        skipConditions,
        new Transformer.Many2Many(
            facetValue -> transformer.apply((FanoutDepResponses<SDV, S>) facetValue)));
  }
}

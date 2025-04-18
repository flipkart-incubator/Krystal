package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.specs.FanoutDepSpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * One of the stages in input resolution DSL
 *
 * @param <S> Source Type: The DataType of the source input being used for resolution
 * @param <T> Target Type: The DataType of the dependency target input being resolved
 * @param <R> Response Type: The data type of the dependency response
 * @param <CV> CurrentVajram: The current vajram which is resolving the input
 * @param <DV> DependencyVajram: The vajram whose input is being resolved
 */
public final class MultiValTransformResolverStage<
    S, T, CV extends Request<?>, R, DV extends Request<R>> {
  private final InputMirrorSpec<T, DV> targetInput;
  private final FanoutDepSpec<S, CV, ?> sourceInput;
  private final List<SkipPredicate> skipConditions = new ArrayList<>();

  MultiValTransformResolverStage(
      InputMirrorSpec<T, DV> targetInput, FanoutDepSpec<S, CV, ?> sourceInput) {
    this.targetInput = targetInput;
    this.sourceInput = sourceInput;
  }

  /**
   * Skip the dependency if the predicate returns true with the given reason
   *
   * @param whenToSkip The condition when the dependency needs to be skipped
   * @param reason The reason for skipping the dependency
   */
  @SuppressWarnings("unchecked")
  public MultiValTransformResolverStage<S, T, CV, R, DV> skipIf(
      Predicate<FanoutDepResponses<R, DV>> whenToSkip, String reason) {
    this.skipConditions.add(
        new SkipPredicate(
            reason, facetValue -> whenToSkip.test((FanoutDepResponses<R, DV>) facetValue)));
    return this;
  }

  /**
   * Final step in the resolver DSL.
   *
   * @param transformer The logic to use to transform the source data type {@code S} to a single
   *     value of target data type {@code T} (no fanout)
   * @return The resultant {@link SimpleInputResolverSpec}
   */
  @SuppressWarnings("unchecked")
  public SimpleInputResolverSpec<T, CV, DV> asResolver(
      Function<FanoutDepResponses<R, DV>, @Nullable T> transformer) {
    return new SimpleInputResolverSpec<>(
        targetInput,
        sourceInput,
        skipConditions,
        new Transformer.Many2One(
            fanoutDepResponses ->
                transformer.apply((FanoutDepResponses<R, DV>) fanoutDepResponses)));
  }
}

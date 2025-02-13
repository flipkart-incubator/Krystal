package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.google.common.collect.ImmutableSet;
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
 * @param <CV> CurrentVajram: The current vajram which is resolving the input
 * @param <DV> DependencyVajram: The vajram whose input is being resolved
 */
public final class TransformResolverStage<S, T, CV extends Request, DV extends Request> {
  private final InputMirrorSpec<T, DV> targetInput;
  private final FacetSpec<S, CV> sourceInput;
  private final List<SkipPredicate> skipConditions = new ArrayList<>();

  TransformResolverStage(InputMirrorSpec<T, DV> targetInput, FacetSpec<S, CV> sourceInput) {
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
  public TransformResolverStage<S, T, CV, DV> skipIf(
      Predicate<Errable<S>> whenToSkip, String reason) {
    this.skipConditions.add(
        new SkipPredicate(reason, errable -> whenToSkip.test((Errable<S>) errable.get(0))));
    return this;
  }

  /**
   * Final step in the resolver DSL.
   *
   * @param transformer The logic to use to tranform the source data type {@code S} to a single
   *     value of target data type {@code T} (no fanout)
   * @return The resultant {@link SimpleInputResolverSpec}
   */
  @SuppressWarnings("unchecked")
  public SimpleInputResolverSpec<T, CV, DV> asResolver(
      Function<Errable<S>, @Nullable T> transformer) {
    return new SimpleInputResolverSpec<>(
        targetInput,
        ImmutableSet.of(sourceInput),
        skipConditions,
        new Transformer.One2One(
            list -> {
              return transformer.apply((Errable<S>) list.get(0));
            }));
  }
}

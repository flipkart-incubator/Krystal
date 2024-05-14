package com.flipkart.krystal.vajram.facets.resolution.sdk;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolverSpec;
import com.flipkart.krystal.vajram.facets.resolution.SkipPredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @param <S> Source Type: The DataType of the source input being used for resolution
 * @param <T> Target Type: The DataType of the dependency target input being resolved
 * @param <CV> CurrentVajram: The current vajram which is resolving the input
 * @param <DV> DependencyVajram: The vajram whose input is being resolved
 */
public final class Transform1ResolverStage<S, T, CV extends Request<?>, DV extends Request<?>> {
  private final VajramFacetSpec<T, DV> targetInput;
  private final VajramFacetSpec<S, CV> sourceInput;
  private final List<SkipPredicate<?>> skipConditions = new ArrayList<>();

  Transform1ResolverStage(VajramFacetSpec<T, DV> targetInput, VajramFacetSpec<S, CV> sourceInput) {
    this.targetInput = targetInput;
    this.sourceInput = sourceInput;
  }

  /**
   * @param whenToSkip The condition when the dependency needs to be skipped
   * @param reason The reason for skipping the dependency
   */
  public Transform1ResolverStage<S, T, CV, DV> skipIf(
      Predicate<Errable<S>> whenToSkip, String reason) {
    //noinspection unchecked
    this.skipConditions.add(
        new SkipPredicate<>(reason, errable -> whenToSkip.test((Errable<S>) errable.get(0))));
    return this;
  }

  /**
   * @param transformer The logic to use to tranform the source data type {@code S} to a single
   *     value of target data type {@code T} (no fanout)
   * @return The resultant {@link SimpleInputResolverSpec}
   */
  public SimpleInputResolverSpec<T, CV, DV> asResolver(
      Function<Errable<S>, @Nullable T> transformer) {
    return new SimpleInputResolverSpec<>(
        targetInput,
        List.of(sourceInput),
        skipConditions,
        list -> {
          //noinspection unchecked
          return transformer.apply((Errable<S>) list.get(0));
        },
        null);
  }
}

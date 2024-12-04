package com.flipkart.krystal.vajram.facets.resolution.sdk;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolverSpec;
import com.flipkart.krystal.vajram.facets.resolution.SkipPredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @param <S1> Source Type: The DataType of the first source input being used for resolution
 * @param <S2> Source Type: The DataType of the second source input being used for resolution
 * @param <T> Target Type: The DataType of the dependency target input being resolved
 * @param <CV> CurrentVajram: The current vajram which is resolving the input
 * @param <DV> DependencyVajram: The vajram whose input is being resolved
 */
public final class Transform2ResolverStage<
    S1, S2, T, CV extends VajramRequest<?>, DV extends VajramRequest<?>> {
  private final VajramFacetSpec<T, DV> targetInput;
  private final VajramFacetSpec<S1, CV> sourceInput1;
  private final VajramFacetSpec<S2, CV> sourceInput2;
  private final List<SkipPredicate<?>> skipConditions = new ArrayList<>();

  Transform2ResolverStage(
      VajramFacetSpec<T, DV> targetInput,
      VajramFacetSpec<S1, CV> sourceInput1,
      VajramFacetSpec<S2, CV> sourceInput2) {
    this.targetInput = targetInput;
    this.sourceInput1 = sourceInput1;
    this.sourceInput2 = sourceInput2;
  }

  /**
   * @param whenToSkip The condition when the dependency needs to be skipped
   * @param reason The reason for skipping the dependency
   */
  @SuppressWarnings("unchecked")
  public Transform2ResolverStage<S1, S2, T, CV, DV> skipIf(
      BiPredicate<Errable<S1>, Errable<S2>> whenToSkip, String reason) {
    this.skipConditions.add(
        new SkipPredicate<>(
            reason,
            errables ->
                whenToSkip.test((Errable<S1>) errables.get(0), (Errable<S2>) errables.get(1))));
    return this;
  }

  /**
   * @param transformer The logic to use to tranform the source data type {@code S} to a single
   *     value of target data type {@code T} (no fanout)
   * @return The resultant {@link SimpleInputResolverSpec}
   */
  @SuppressWarnings("unchecked")
  public SimpleInputResolverSpec<T, CV, DV> asResolver(
      BiFunction<Errable<S1>, Errable<S2>, @Nullable T> transformer) {
    return new SimpleInputResolverSpec<>(
        targetInput,
        List.of(sourceInput1, sourceInput2),
        skipConditions,
        list -> {
          return transformer.apply((Errable<S1>) list.get(0), (Errable<S2>) list.get(1));
        },
        null);
  }
}

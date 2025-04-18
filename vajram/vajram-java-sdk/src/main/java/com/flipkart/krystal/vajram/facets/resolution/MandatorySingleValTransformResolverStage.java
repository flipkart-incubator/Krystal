package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.FacetValue.SingleFacetValue;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.exception.MandatoryFacetMissingException;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.flipkart.krystal.vajram.facets.specs.MandatorySingleValueFacetSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * One of the stages in input resolution DSL
 *
 * @param <S> Source Type: The DataType of the source input being used for resolution
 * @param <T> Target Type: The DataType of the dependency target input being resolved
 * @param <CV> CurrentVajram: The current vajram which is resolving the input
 * @param <DV> DependencyVajram: The vajram whose input is being resolved
 */
public final class MandatorySingleValTransformResolverStage<
    S, T, CV extends Request, DV extends Request> {
  private final InputMirrorSpec<T, DV> targetInput;
  private final MandatorySingleValueFacetSpec<S, CV> sourceFacet;
  private final List<SkipPredicate> skipConditions = new ArrayList<>();

  MandatorySingleValTransformResolverStage(
      InputMirrorSpec<T, DV> targetInput, MandatorySingleValueFacetSpec<S, CV> sourceFacet) {
    this.targetInput = targetInput;
    this.sourceFacet = sourceFacet;
  }

  /**
   * Skip the dependency if the predicate returns true with the given reason
   *
   * @param whenToSkip The condition when the dependency needs to be skipped
   * @param reason The reason for skipping the dependency
   */
  @SuppressWarnings("unchecked")
  public MandatorySingleValTransformResolverStage<S, T, CV, DV> skipIf(
      Predicate<S> whenToSkip, String reason) {
    this.skipConditions.add(
        new SkipPredicate(
            reason,
            facetValue ->
                whenToSkip.test(
                    (S)
                        ((SingleFacetValue<?>) facetValue)
                            .singleValue()
                            .valueOpt()
                            .orElseThrow(mandatoryFacetMissingException()))));
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
  public SimpleInputResolverSpec<T, CV, DV> asResolver(Function<S, @Nullable T> transformer) {
    return new SimpleInputResolverSpec<>(
        targetInput,
        sourceFacet,
        skipConditions,
        new Transformer.One2One(
            facetValue ->
                transformer.apply(
                    (S)
                        facetValue
                            .singleValue()
                            .valueOpt()
                            .orElseThrow(mandatoryFacetMissingException()))));
  }

  private Supplier<MandatoryFacetMissingException> mandatoryFacetMissingException() {
    return () ->
        new MandatoryFacetMissingException(sourceFacet.ofVajramID().vajramId(), sourceFacet.name());
  }
}

package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.FacetValue.SingleFacetValue;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.exception.MandatoryFacetMissingException;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.flipkart.krystal.vajram.facets.specs.MandatorySingleValueFacetSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class MandatoryAsIsResolverStage<T, CV extends Request, DV extends Request> {
  private final InputMirrorSpec<T, DV> targetInput;
  private final MandatorySingleValueFacetSpec<T, CV> sourceFacet;
  private final List<SkipPredicate> skipConditions = new ArrayList<>();

  MandatoryAsIsResolverStage(
      InputMirrorSpec<T, DV> targetInput, MandatorySingleValueFacetSpec<T, CV> sourceFacet) {
    this.targetInput = targetInput;
    this.sourceFacet = sourceFacet;
  }

  @SuppressWarnings("unchecked")
  public MandatoryAsIsResolverStage<T, CV, DV> skipIf(Predicate<T> whenToSkip, String reason) {
    this.skipConditions.add(
        new SkipPredicate(
            reason,
            facetValue ->
                whenToSkip.test(
                    ((SingleFacetValue<T>) facetValue)
                        .asErrable()
                        .valueOpt()
                        .orElseThrow(this::mandatoryFacetMissingException))));
    return this;
  }

  @SuppressWarnings("unchecked")
  public SimpleInputResolverSpec<T, CV, DV> asResolver() {
    return new SimpleInputResolverSpec<>(
        targetInput,
        sourceFacet,
        skipConditions,
        new Transformer.One2One(
            t -> t.asErrable().valueOpt().orElseThrow(this::mandatoryFacetMissingException)));
  }

  private MandatoryFacetMissingException mandatoryFacetMissingException() {
    return new MandatoryFacetMissingException(sourceFacet.ofVajramID().id(), sourceFacet.name());
  }
}

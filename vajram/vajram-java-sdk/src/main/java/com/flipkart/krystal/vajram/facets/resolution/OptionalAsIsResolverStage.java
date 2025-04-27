package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue.SingleFacetValue;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.flipkart.krystal.vajram.facets.specs.OptionalSingleValueFacetSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class OptionalAsIsResolverStage<T, CV extends Request, DV extends Request> {
  private final InputMirrorSpec<T, DV> targetInput;
  private final OptionalSingleValueFacetSpec<T, CV> sourceInput;
  private final List<SkipPredicate> skipConditions = new ArrayList<>();

  OptionalAsIsResolverStage(
      InputMirrorSpec<T, DV> targetInput, OptionalSingleValueFacetSpec<T, CV> sourceInput) {
    this.targetInput = targetInput;
    this.sourceInput = sourceInput;
  }

  @SuppressWarnings("unchecked")
  public OptionalAsIsResolverStage<T, CV, DV> skipIf(
      Predicate<Errable<T>> whenToSkip, String reason) {
    this.skipConditions.add(
        new SkipPredicate(
            reason, facetValue -> whenToSkip.test(((SingleFacetValue<T>) facetValue).asErrable())));
    return this;
  }

  @SuppressWarnings("unchecked")
  public SimpleInputResolverSpec<T, CV, DV> asResolver() {
    return new SimpleInputResolverSpec<>(
        targetInput,
        sourceInput,
        skipConditions,
        new Transformer.One2One(t -> t.asErrable().valueOpt().orElse(null)));
  }
}

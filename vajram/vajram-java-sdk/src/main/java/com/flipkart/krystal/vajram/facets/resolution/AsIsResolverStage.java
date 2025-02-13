package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class AsIsResolverStage<T, CV extends Request, DV extends Request> {
  private final InputMirrorSpec<T, DV> targetInput;
  private final FacetSpec<T, CV> sourceInput;
  private final List<SkipPredicate> skipConditions = new ArrayList<>();

  AsIsResolverStage(InputMirrorSpec<T, DV> targetInput, FacetSpec<T, CV> sourceInput) {
    this.targetInput = targetInput;
    this.sourceInput = sourceInput;
  }

  @SuppressWarnings("unchecked")
  public AsIsResolverStage<T, CV, DV> skipIf(Predicate<Errable<T>> whenToSkip, String reason) {
    this.skipConditions.add(
        new SkipPredicate(reason, errables -> whenToSkip.test((Errable<T>) errables.get(0))));
    return this;
  }

  @SuppressWarnings("unchecked")
  public SimpleInputResolverSpec<T, CV, DV> asResolver() {
    return new SimpleInputResolverSpec<>(
        targetInput,
        ImmutableSet.of(sourceInput),
        skipConditions,
        new Transformer.One2One(t -> (T) t.get(0).valueOpt().orElse(null)));
  }
}

package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class AsIsResolverStage<T, CV extends VajramRequest<?>, DV extends VajramRequest<?>> {
  private final VajramFacetSpec<T, DV> targetInput;
  private final VajramFacetSpec<T, CV> sourceInput;
  private final List<SkipPredicate<T>> skipConditions = new ArrayList<>();

  AsIsResolverStage(VajramFacetSpec<T, DV> targetInput, VajramFacetSpec<T, CV> sourceInput) {
    this.targetInput = targetInput;
    this.sourceInput = sourceInput;
  }

  public AsIsResolverStage<T, CV, DV> skipIf(Predicate<Optional<T>> whenToSkip, String reason) {
    this.skipConditions.add(new SkipPredicate<>(reason, whenToSkip));
    return this;
  }

  public SimpleInputResolverSpec<T, T, CV, DV> asResolver() {
    return new SimpleInputResolverSpec<>(
        targetInput, sourceInput, skipConditions, t -> t.orElse(null), null);
  }
}

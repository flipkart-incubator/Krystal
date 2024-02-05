package com.flipkart.krystal.vajram.facets.resolution.sdk;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolverSpec;
import com.flipkart.krystal.vajram.facets.resolution.SkipPredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class AsIsResolverStage<T, CV extends VajramRequest<?>, DV extends VajramRequest<?>> {
  private final VajramFacetSpec<T, DV> targetInput;
  private final VajramFacetSpec<T, CV> sourceInput;
  private final List<SkipPredicate<?>> skipConditions = new ArrayList<>();

  AsIsResolverStage(VajramFacetSpec<T, DV> targetInput, VajramFacetSpec<T, CV> sourceInput) {
    this.targetInput = targetInput;
    this.sourceInput = sourceInput;
  }

  public AsIsResolverStage<T, CV, DV> skipIf(Predicate<ValueOrError<T>> whenToSkip, String reason) {
    //noinspection unchecked
    this.skipConditions.add(
        new SkipPredicate<>(
            reason, valueOrErrors -> whenToSkip.test((ValueOrError<T>) valueOrErrors.get(0))));
    return this;
  }

  public SimpleInputResolverSpec<T, CV, DV> asResolver() {
    //noinspection unchecked
    return new SimpleInputResolverSpec<>(
        targetInput,
        List.of(sourceInput),
        skipConditions,
        t -> (T) t.get(0).value().orElse(null),
        null);
  }
}

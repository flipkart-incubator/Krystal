package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.facets.FacetType.INJECTION;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.google.common.collect.ImmutableSet;
import lombok.Value;

@Value
public final class VajramMetadata {

  boolean isInputInjectionNeeded;
  boolean isBatched;

  VajramMetadata(ImmutableSet<FacetSpec> facetSpecs) {
    this.isInputInjectionNeeded =
        facetSpecs.stream().map(Facet::facetType).anyMatch(INJECTION::equals);
    this.isBatched = facetSpecs.stream().anyMatch(FacetSpec::isBatched);
  }
}

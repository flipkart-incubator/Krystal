package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.facets.FacetType.INJECTION;

import com.flipkart.krystal.annos.ComputeDelegationMode;
import com.flipkart.krystal.annos.OutputLogicDelegationMode;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.google.common.collect.ImmutableSet;

public record VajramMetadata(
    boolean isInputInjectionNeeded, boolean isBatched, boolean isComputeVajram) {

  VajramMetadata(ImmutableSet<FacetSpec> facetSpecs, ElementTags vajramTags) {
    this(
        facetSpecs.stream().map(Facet::facetType).anyMatch(INJECTION::equals),
        facetSpecs.stream().anyMatch(FacetSpec::isBatched),
        ComputeDelegationMode.NONE
            == vajramTags
                .getAnnotationByType(OutputLogicDelegationMode.class)
                .map(OutputLogicDelegationMode::value)
                .orElse(null));
  }
}

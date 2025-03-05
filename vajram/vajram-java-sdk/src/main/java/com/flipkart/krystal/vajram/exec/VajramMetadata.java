package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.facets.Dependency;
import static com.flipkart.krystal.facets.FacetType.INJECTION;

import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.facets.TraitDependency;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.google.common.collect.ImmutableSet;

public record VajramMetadata(boolean isInputInjectionNeeded, boolean isBatched) {

  public VajramMetadata(VajramDefRoot<?> vajramDef, ImmutableSet<FacetSpec> facetSpecs) {
    this(
        /* isInputInjectionNeeded= */ facetSpecs.stream()
            .anyMatch(facetDefinition -> facetDefinition.facetTypes().contains(INJECTION)),
        facetSpecs.stream().anyMatch(FacetSpec::isBatched));
  }
}

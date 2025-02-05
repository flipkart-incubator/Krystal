package com.flipkart.krystal.facets;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.google.common.collect.ImmutableSet;

public interface Dependency extends Facet {

  ImmutableSet<FacetType> DEP_FACET_TYPES = ImmutableSet.of(DEPENDENCY);

  boolean canFanout();

  @Override
  default ImmutableSet<FacetType> facetTypes() {
    return DEP_FACET_TYPES;
  }
}

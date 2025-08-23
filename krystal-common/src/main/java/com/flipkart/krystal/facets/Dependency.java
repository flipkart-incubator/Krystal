package com.flipkart.krystal.facets;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

public interface Dependency extends Facet {

  boolean canFanout();

  @Override
  default FacetType facetType() {
    return DEPENDENCY;
  }
}

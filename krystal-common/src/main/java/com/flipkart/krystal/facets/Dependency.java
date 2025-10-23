package com.flipkart.krystal.facets;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.flipkart.krystal.core.VajramID;

public interface Dependency extends Facet {

  boolean canFanout();

  VajramID onVajramID();

  @Override
  default FacetType facetType() {
    return DEPENDENCY;
  }
}

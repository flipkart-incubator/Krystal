package com.flipkart.krystal.krystex.testutils;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.facets.Dependency;

public final class SimpleDep extends SimpleFacet implements Dependency {

  SimpleDep(int id, String name) {
    super(id, name, DEPENDENCY);
  }

  public void setToFacets(FacetValues facetValues, DepResponse value) {
    ((FacetValuesMapBuilder) facetValues._asBuilder())._set(id(), value);
  }

  @Override
  public boolean canFanout() {
    return false;
  }
}

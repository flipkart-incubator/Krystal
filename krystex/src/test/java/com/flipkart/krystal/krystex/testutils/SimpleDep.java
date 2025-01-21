package com.flipkart.krystal.krystex.testutils;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FacetsMap;
import com.flipkart.krystal.facets.Dependency;

public final class SimpleDep extends SimpleFacet implements Dependency {

  SimpleDep(int id, String name) {
    super(id, name, DEPENDENCY);
  }

  @Override
  public FacetValue getFacetValue(Facets facets) {
    return null;
  }

  @Override
  public void setToFacets(Facets facets, DepResponse value) {
    ((FacetsMap) facets)._asBuilder()._set(id(), value);
  }

  @Override
  public boolean canFanout() {
    return false;
  }
}

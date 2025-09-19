package com.flipkart.krystal.krystex.testutils;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.facets.Dependency;
import lombok.Getter;

public final class SimpleDep extends SimpleFacet implements Dependency {

  @Getter private final VajramID ofVajramID;
  @Getter private final VajramID onVajramID;

  SimpleDep(int id, String name, VajramID ofVajramID, VajramID onVajramID) {
    super(id, name, DEPENDENCY);
    this.ofVajramID = ofVajramID;
    this.onVajramID = onVajramID;
  }

  public void setToFacets(FacetValues facetValues, DepResponse value) {
    ((FacetValuesMapBuilder) facetValues._asBuilder())._set(id(), value);
  }

  @Override
  public boolean canFanout() {
    return false;
  }
}

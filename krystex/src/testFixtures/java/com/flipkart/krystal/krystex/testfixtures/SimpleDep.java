package com.flipkart.krystal.krystex.testfixtures;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.facets.Dependency;
import java.util.Objects;

public final class SimpleDep extends SimpleFacet implements Dependency {

  private final VajramID ofVajramID;
  private final VajramID onVajramID;

  SimpleDep(int id, String name, VajramID ofVajramID, VajramID onVajramID) {
    super(id, name, DEPENDENCY);
    this.ofVajramID = ofVajramID;
    this.onVajramID = onVajramID;
  }

  @Override
  public VajramID ofVajramID() {
    return ofVajramID;
  }

  @Override
  public VajramID onVajramID() {
    return onVajramID;
  }

  public void setToFacets(FacetValues facetValues, DepResponse value) {
    ((FacetValuesMapBuilder) facetValues._asBuilder())._set(id(), value);
  }

  @Override
  public boolean canFanout() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    SimpleDep that = (SimpleDep) o;
    return id() == that.id() && facetType() == that.facetType();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id(), facetType());
  }
}

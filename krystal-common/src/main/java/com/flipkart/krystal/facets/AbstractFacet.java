package com.flipkart.krystal.facets;

import com.flipkart.krystal.core.VajramID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class AbstractFacet implements Facet {
  private final int id;
  private final String name;
  private final VajramID ofVajramID;
  private final FacetType facetType;
  private final String documentation;

  @Override
  public String toString() {
    return "Facet(" + name + ')';
  }
}

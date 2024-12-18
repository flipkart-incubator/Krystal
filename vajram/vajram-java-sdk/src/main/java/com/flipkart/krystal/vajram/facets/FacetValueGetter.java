package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;

/** A function which can get the value of a particular facet from a facets object */
@FunctionalInterface
public interface FacetValueGetter {
  FacetValue setValue(Facets facets);
}

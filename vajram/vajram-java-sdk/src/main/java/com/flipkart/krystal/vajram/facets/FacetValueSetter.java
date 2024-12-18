package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;

/** A function which can set a value to a particular facet in a facets object */
@FunctionalInterface
public interface FacetValueSetter {
  void setValue(Facets facets, FacetValue value);
}

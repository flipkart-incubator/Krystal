package com.flipkart.krystal.facets;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;

public interface Facet extends BasicFacetInfo {

  FacetType facetType();

  FacetValue<?> getFacetValue(FacetValues facetValues);

  void setFacetValue(FacetValuesBuilder facets, FacetValue<?> value);
}

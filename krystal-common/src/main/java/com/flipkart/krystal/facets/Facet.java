package com.flipkart.krystal.facets;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Facet extends BasicFacetInfo {

  ImmutableSet<FacetType> facetTypes();

  @Nullable FacetValue getFacetValue(FacetValues facetValues);

  void setFacetValue(FacetValuesBuilder facets, FacetValue value);
}

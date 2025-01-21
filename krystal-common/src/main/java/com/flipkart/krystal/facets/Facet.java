package com.flipkart.krystal.facets;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FacetsBuilder;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Facet extends BasicFacetInfo {

  ImmutableSet<FacetType> facetTypes();

  @Nullable FacetValue getFacetValue(Facets facets);

  void setFacetValue(FacetsBuilder facets, FacetValue value);
}

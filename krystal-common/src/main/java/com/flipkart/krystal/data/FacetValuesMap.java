package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;

public sealed interface FacetValuesMap extends FacetValues
    permits FacetValuesMapBuilder, ImmutableFacetValuesMap {

  Errable<?> _getErrable(int facetId);

  @SuppressWarnings("unchecked")
  FanoutDepResponses<?, ?> _getDepResponses(int facetId);

  ImmutableMap<Integer, FacetValue> _asMap();
}

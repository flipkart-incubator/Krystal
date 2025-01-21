package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;

public sealed interface FacetsMap extends Facets permits FacetsMapBuilder, ImmutableFacetsMap {

  Errable<?> _getErrable(int facetId);

  @SuppressWarnings("unchecked")
  FanoutDepResponses _getDepResponses(int facetId);

  ImmutableMap<Integer, FacetValue> _asMap();
}

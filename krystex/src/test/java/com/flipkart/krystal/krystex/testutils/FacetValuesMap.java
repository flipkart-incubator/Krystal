package com.flipkart.krystal.krystex.testutils;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.google.common.collect.ImmutableMap;

public sealed interface FacetValuesMap extends FacetValues
    permits FacetValuesMapBuilder, ImmutableFacetValuesMap {

  Errable<?> _getErrable(int facetId);

  @SuppressWarnings("unchecked")
  FanoutDepResponses<?, ?> _getDepResponses(int facetId);

  ImmutableMap<Integer, FacetValue> _asMap();
}

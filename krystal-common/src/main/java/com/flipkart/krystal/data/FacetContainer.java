package com.flipkart.krystal.data;

import java.util.Map;

/**
 * Marker interface extended by auto-generated classes holding facet values of a vajram. This class
 * is intended for use by the krystal platform code generator. Developers must not implement/extend
 * this interface - this can lead to unexpected behaviour.
 */
public sealed interface FacetContainer extends Model permits Facets, Request {
  FacetValue<?> _get(int facetId);

  Map<Integer, ? extends FacetValue<?>> _asMap();
}

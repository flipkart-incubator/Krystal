package com.flipkart.krystal.data;

import java.util.Map;

/**
 * Marker interface extended by auto-generated classes holding facet values of a vajram. This class
 * is intended for use by the krystal platform code generator. Developers must not implement/extend
 * this interface - this can lead to unexpected behaviour.
 */
public interface FacetContainer extends Model {
  <V> FacetValue<V> _get(int facetId);

  <V> Errable<V> _getErrable(int facetId);

  <R extends Request<V>, V> Responses<R, V> _getResponses(int facetId);

  Map<Integer, ? extends FacetValue<Object>> _asMap();

  boolean _hasValue(int facetId);
}

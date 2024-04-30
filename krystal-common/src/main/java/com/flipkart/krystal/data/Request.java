package com.flipkart.krystal.data;

import java.util.Map;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
@SuppressWarnings("ClassReferencesSubclass") // By Design
public sealed interface Request<T> extends FacetContainer permits ImmutableRequest, RequestBuilder {

  @Override
  Errable<Object> _get(int facetId);

  ImmutableRequest<T> _build();

  @Override
  RequestBuilder<T> _asBuilder();

  @Override
  Request<T> _newCopy();

  Map<Integer, Errable<Object>> _asMap();
}

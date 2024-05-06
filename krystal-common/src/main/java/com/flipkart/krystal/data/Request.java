package com.flipkart.krystal.data;

import java.util.Map;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
@SuppressWarnings("ClassReferencesSubclass") // By Design
public non-sealed interface Request<T> extends FacetContainer {

  @Override
  Errable<?> _get(int facetId);

  ImmutableRequest<T> _build();

  @Override
  RequestBuilder<T> _asBuilder();

  @Override
  Request<T> _newCopy();

  Map<Integer, ? extends Errable<?>> _asMap();
}

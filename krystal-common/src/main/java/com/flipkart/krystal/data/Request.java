package com.flipkart.krystal.data;

import com.flipkart.krystal.facets.RemoteInput;
import com.google.common.collect.ImmutableSet;

@SuppressWarnings("ClassReferencesSubclass") // By Design
public interface Request<T> extends FacetContainer {

  ImmutableRequest<T> _build();

  RequestBuilder<T> _asBuilder();

  Request<T> _newCopy();

  /** Returns the facet definitions of the all the inputs of the vajram. */
  @Override
  ImmutableSet<? extends RemoteInput> _facets();
}

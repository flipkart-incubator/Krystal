package com.flipkart.krystal.data;

import com.flipkart.krystal.facets.InputMirror;
import com.google.common.collect.ImmutableSet;

@SuppressWarnings("ClassReferencesSubclass") // By Design
public interface Request<T> extends FacetValuesContainer {

  ImmutableRequest<T> _build();

  ImmutableRequest.Builder<T> _asBuilder();

  Request<T> _newCopy();

  /** Returns the facet definitions of the all the inputs of the vajram. */
  @Override
  ImmutableSet<? extends InputMirror> _facets();
}

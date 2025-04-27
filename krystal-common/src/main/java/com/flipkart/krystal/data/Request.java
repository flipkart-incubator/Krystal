package com.flipkart.krystal.data;

import com.flipkart.krystal.facets.InputMirror;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelClusterRoot;
import com.google.common.collect.ImmutableSet;

/**
 * Represents a request to a vajram
 *
 * @param <T> The response type of the vajram when invoked with this request
 */
@SuppressWarnings("ClassReferencesSubclass") // By Design
@ModelClusterRoot(
    immutableRoot = ImmutableRequest.class,
    builderRoot = ImmutableRequest.Builder.class)
public non-sealed interface Request<T> extends FacetValuesContainer, Model {

  ImmutableRequest.Builder<T> _asBuilder();

  ImmutableRequest<T> _build();

  Request<T> _newCopy();

  /** Returns the facet definitions of the all the inputs of the vajram. */
  @Override
  ImmutableSet<? extends InputMirror> _facets();
}

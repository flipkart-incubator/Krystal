package com.flipkart.krystal.data;

import com.flipkart.krystal.facets.Facet;
import com.google.common.collect.ImmutableSet;

/** An facets object contains some or all the facet values of a given Vajram/Kryon. */
@SuppressWarnings("ClassReferencesSubclass") // This is by design.
public non-sealed interface FacetValues extends FacetValuesContainer, Model {

  @Override
  ImmutableFacetValues _build();

  @Override
  FacetValuesBuilder _asBuilder();

  @Override
  FacetValues _newCopy();

  /** Returns the facet definitions of the facets whose values this container potentially holds. */
  @Override
  ImmutableSet<? extends Facet> _facets();
}

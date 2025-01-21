package com.flipkart.krystal.data;

import com.flipkart.krystal.facets.Facet;
import com.google.common.collect.ImmutableSet;

/** An facets object contains some or all the facet values of a given Vajram/Kryon. */
@SuppressWarnings("ClassReferencesSubclass") // This is by design.
public non-sealed interface Facets extends FacetContainer, Model {

  @Override
  ImmutableFacets _build();

  @Override
  FacetsBuilder _asBuilder();

  @Override
  Facets _newCopy();

  /** Returns the facet definitions of the facets whose values this container potentially holds. */
  @Override
  ImmutableSet<? extends Facet> _facets();
}

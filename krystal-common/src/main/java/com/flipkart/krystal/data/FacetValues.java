package com.flipkart.krystal.data;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.model.Model;
import com.google.common.collect.ImmutableSet;

/** A facet values object contains the values of all the facets a given Vajram. */
@SuppressWarnings("ClassReferencesSubclass") // This is by design.
public non-sealed interface FacetValues extends FacetValuesContainer, Model {

  @Override
  ImmutableFacetValues _build();

  @Override
  FacetValuesBuilder _asBuilder();

  @Override
  FacetValues _newCopy();

  /** Returns the request sent to the vajram whose facets this class represents. */
  Request<?> _request();

  /** Returns the facet definitions of the facets whose values this container potentially holds. */
  @Override
  ImmutableSet<? extends Facet> _facets();
}

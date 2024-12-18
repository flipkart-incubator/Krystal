package com.flipkart.krystal.data;

/** An facets object contains all the facets of a given Vajram/Kryon. */
@SuppressWarnings("ClassReferencesSubclass") // This is by design.
public non-sealed interface Facets extends FacetContainer {
  Errable<?> _getErrable(int facetId);

  DependencyResponses<Request<Object>, Object> _getDepResponses(int facetId);

  @Override
  ImmutableFacets _build();

  @Override
  FacetsBuilder _asBuilder();

  @Override
  Facets _newCopy();
}

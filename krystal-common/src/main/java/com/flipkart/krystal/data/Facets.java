package com.flipkart.krystal.data;

@SuppressWarnings("ClassReferencesSubclass") // This is by design.
public non-sealed interface Facets extends FacetContainer {
  Errable<Object> _getErrable(int facetId);

  Responses<Request<Object>, Object> _getDepResponses(int facetId);

  @Override
  ImmutableFacets _build();

  @Override
  FacetsBuilder _asBuilder();

  @Override
  Facets _newCopy();
}

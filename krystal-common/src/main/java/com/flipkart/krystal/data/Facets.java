package com.flipkart.krystal.data;

@SuppressWarnings("ClassReferencesSubclass") // This is by design.
public interface Facets extends FacetContainer {

  ImmutableFacets _build();

  @Override
  FacetsBuilder _asBuilder();

  @Override
  Facets _newCopy();
}

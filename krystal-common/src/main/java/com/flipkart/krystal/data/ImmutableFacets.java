package com.flipkart.krystal.data;

public interface ImmutableFacets extends Facets, ImmutableFacetContainer, ImmutableModel {

  @Override
  ImmutableFacets _build();

  @Override
  ImmutableFacets _newCopy();
}

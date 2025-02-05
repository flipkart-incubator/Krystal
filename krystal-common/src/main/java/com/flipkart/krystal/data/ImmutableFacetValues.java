package com.flipkart.krystal.data;

public interface ImmutableFacetValues
    extends FacetValues, ImmutableFacetValuesContainer, ImmutableModel {

  @Override
  ImmutableFacetValues _build();

  @Override
  ImmutableFacetValues _newCopy();
}

package com.flipkart.krystal.data;

import com.flipkart.krystal.model.ImmutableModel;

public interface ImmutableFacetValues
    extends FacetValues, ImmutableFacetValuesContainer, ImmutableModel {

  @Override
  ImmutableFacetValues _build();

  @Override
  ImmutableFacetValues _newCopy();

  @Override
  ImmutableRequest<?> _request();
}

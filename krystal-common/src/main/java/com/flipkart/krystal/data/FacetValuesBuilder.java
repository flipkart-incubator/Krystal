package com.flipkart.krystal.data;

import com.flipkart.krystal.model.ImmutableModel;

public interface FacetValuesBuilder
    extends FacetValues, FacetValuesContainerBuilder, ImmutableModel.Builder {

  @Override
  default FacetValuesBuilder _asBuilder() {
    return this;
  }

  @Override
  FacetValuesBuilder _newCopy();
}

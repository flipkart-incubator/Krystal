package com.flipkart.krystal.data;

import com.flipkart.krystal.model.ModelBuilder;

public interface FacetValuesBuilder extends FacetValues, FacetValuesContainerBuilder, ModelBuilder {

  @Override
  default FacetValuesBuilder _asBuilder() {
    return this;
  }

  @Override
  FacetValuesBuilder _newCopy();
}

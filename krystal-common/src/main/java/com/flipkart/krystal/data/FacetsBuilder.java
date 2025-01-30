package com.flipkart.krystal.data;

public interface FacetsBuilder extends Facets, FacetContainerBuilder, ModelBuilder {

  @Override
  default FacetsBuilder _asBuilder() {
    return this;
  }

  @Override
  FacetsBuilder _newCopy();
}

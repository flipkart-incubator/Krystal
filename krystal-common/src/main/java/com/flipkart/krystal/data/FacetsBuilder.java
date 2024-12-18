package com.flipkart.krystal.data;

public interface FacetsBuilder extends Facets, FacetContainerBuilder {

  @Override
  FacetsBuilder _asBuilder();

  @Override
  FacetsBuilder _newCopy();

  @Override
  default FacetsBuilder _set(int facetId, FacetValue value) {
    return this;
  }
}

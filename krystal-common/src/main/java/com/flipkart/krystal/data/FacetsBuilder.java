package com.flipkart.krystal.data;

public interface FacetsBuilder extends Facets, FacetContainerBuilder {

  @Override
  FacetsBuilder _asBuilder();

  RequestBuilder<Object> _asRequest();

  @Override
  FacetsBuilder _newCopy();

  @Override
  FacetsBuilder _set(int facetId, FacetValue<?> value);
}

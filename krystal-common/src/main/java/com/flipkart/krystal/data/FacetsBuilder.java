package com.flipkart.krystal.data;

public abstract class FacetsBuilder implements Facets, FacetContainerBuilder {

  @Override
  public FacetsBuilder _asBuilder() {
    return this;
  }

  public abstract RequestBuilder<Object> _asRequest();

  @Override
  public abstract FacetsBuilder _newCopy();
}

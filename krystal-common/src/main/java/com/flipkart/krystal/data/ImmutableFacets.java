package com.flipkart.krystal.data;

public abstract class ImmutableFacets implements Facets, ImmutableModel {

  @Override
  public ImmutableFacets _build() {
    return this;
  }

  public final ImmutableFacets _newCopy() {
    return this;
  }

  public abstract ImmutableRequest<Object> _asRequest();
}

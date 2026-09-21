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

  @Override
  ImmutableRequest.Builder<?> _request();

  /**
   * Copies the {@code INJECTION}-typed facets from {@code injectedFacets} onto this builder.
   * Vajrams with no {@code INJECTION} facets keep the no-op default; vajrams which have them get a
   * generated override.
   *
   * @param injectedFacets a builder of the same concrete type as this one, populated only with
   *     {@code INJECTION} facets (e.g. produced by a natively-injected {@code
   *     @jakarta.inject.Inject} constructor)
   */
  default FacetValuesBuilder _mergeInjectedFacetsFrom(FacetValuesBuilder injectedFacets) {
    return this;
  }
}

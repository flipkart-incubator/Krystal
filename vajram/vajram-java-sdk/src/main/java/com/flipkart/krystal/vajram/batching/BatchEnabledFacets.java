package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableFacetContainer;

@SuppressWarnings("ClassReferencesSubclass") // By Design
public interface BatchEnabledFacets extends Facets {
  ImmutableFacetContainer _batchElement();

  ImmutableFacetContainer _common();

  @Override
  BatchEnabledImmutableFacets _build();
}

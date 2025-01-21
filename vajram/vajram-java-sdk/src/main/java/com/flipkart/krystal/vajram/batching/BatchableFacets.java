package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetContainer;
import com.flipkart.krystal.data.Facets;

@SuppressWarnings("ClassReferencesSubclass") // By Design
public interface BatchableFacets extends Facets {
  FacetContainer _batchable();

  FacetContainer _common();

  @Override
  BatchableImmutableFacets _build();
}

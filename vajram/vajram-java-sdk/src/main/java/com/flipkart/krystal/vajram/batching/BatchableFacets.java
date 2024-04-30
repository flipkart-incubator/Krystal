package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.Facets;

@SuppressWarnings("ClassReferencesSubclass") // By Design
public interface BatchableFacets extends Facets {
  Facets _batchable();

  Facets _common();

  @Override
  BatchableImmutableFacets _build();
}

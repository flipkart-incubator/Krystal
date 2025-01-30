package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.ImmutableFacetContainer;

public interface BatchedFacetsElement extends ImmutableFacetContainer {
  BatchEnabledImmutableFacets _allFacetValues();
}

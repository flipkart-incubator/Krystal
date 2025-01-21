package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.ImmutableFacetContainer;
import com.flipkart.krystal.data.ImmutableFacets;

public interface BatchableImmutableFacets extends BatchableFacets, ImmutableFacets {

  ImmutableFacetContainer _batchable();

  ImmutableFacetContainer _common();

  @Override
  BatchableImmutableFacets _newCopy();
}

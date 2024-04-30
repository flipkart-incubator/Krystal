package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.ImmutableFacets;

public interface BatchableImmutableFacets extends BatchableFacets, ImmutableFacets {

  ImmutableFacets _batchable();

  ImmutableFacets _common();
}

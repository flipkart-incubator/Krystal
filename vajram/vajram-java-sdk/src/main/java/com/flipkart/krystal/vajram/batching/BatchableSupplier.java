package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.Facets;

@FunctionalInterface
public interface BatchableSupplier<BatchedFacets extends Facets, CommonFacets extends Facets> {

  /** Creates an instance of unbatched facets from single batch and common facets. */
  BatchableFacets<BatchedFacets, CommonFacets> createBatchable(
      BatchedFacets batched, CommonFacets common);
}

package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.Facets;

@FunctionalInterface
public interface BatchableSupplier<BatchFacets extends Facets, CommonFacets extends Facets> {

  /** Creates an instance of unbatched facets from single batch and common facets. */
  BatchableFacets createBatchable(BatchFacets batch, CommonFacets common);
}

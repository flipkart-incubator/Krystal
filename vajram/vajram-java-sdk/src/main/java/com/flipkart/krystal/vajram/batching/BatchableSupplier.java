package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetContainer;

@FunctionalInterface
public interface BatchableSupplier<
    BatchFacets extends FacetContainer, CommonFacets extends FacetContainer> {

  /** Creates an instance of unbatched facets from single batch and common facets. */
  BatchableFacets createBatchable(BatchFacets batch, CommonFacets common);
}

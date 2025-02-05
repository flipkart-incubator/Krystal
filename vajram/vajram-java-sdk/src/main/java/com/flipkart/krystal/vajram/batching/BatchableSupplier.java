package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetValuesContainer;

@FunctionalInterface
public interface BatchableSupplier<
    BatchFacets extends FacetValuesContainer, CommonFacets extends FacetValuesContainer> {

  /** Creates an instance of unbatched facets from single batch and common facets. */
  BatchEnabledFacetValues createBatchable(BatchFacets batch, CommonFacets common);
}

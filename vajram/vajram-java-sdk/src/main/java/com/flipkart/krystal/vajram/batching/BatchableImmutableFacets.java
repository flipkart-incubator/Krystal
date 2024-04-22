package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetContainer;
import com.flipkart.krystal.data.ImmutableFacets;

public abstract class BatchableImmutableFacets<
        Batchable extends FacetContainer, Common extends FacetContainer>
    extends ImmutableFacets implements BatchableFacets<Batchable, Common> {

  @Override
  public BatchableImmutableFacets<Batchable, Common> _build() {
    return this;
  }
}

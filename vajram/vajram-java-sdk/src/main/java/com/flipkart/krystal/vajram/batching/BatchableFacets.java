package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetContainer;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableFacets;

@SuppressWarnings("ClassReferencesSubclass") // By design
public interface BatchableFacets<Batchable extends FacetContainer, Common extends FacetContainer>
    extends Facets {

  Batchable _batchable();

  Common _common();

  @Override
  BatchableImmutableFacets<Batchable, Common> _build();
}

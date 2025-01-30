package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetsBuilder;
import com.flipkart.krystal.data.ImmutableFacets;

public interface BatchEnabledImmutableFacets extends BatchEnabledFacets, ImmutableFacets {

  @Override
  BatchEnabledImmutableFacets _newCopy();

  interface Builder extends BatchEnabledFacets, FacetsBuilder {}
}

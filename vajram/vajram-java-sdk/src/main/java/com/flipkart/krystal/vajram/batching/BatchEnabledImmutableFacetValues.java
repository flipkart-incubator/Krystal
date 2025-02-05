package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.ImmutableFacetValues;

public interface BatchEnabledImmutableFacetValues
    extends BatchEnabledFacetValues, ImmutableFacetValues {

  @Override
  BatchEnabledImmutableFacetValues _newCopy();

  interface Builder extends BatchEnabledFacetValues, FacetValuesBuilder {}
}

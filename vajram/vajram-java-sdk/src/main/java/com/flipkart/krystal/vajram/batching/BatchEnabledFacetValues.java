package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableFacetValuesContainer;

@SuppressWarnings("ClassReferencesSubclass") // By Design
public interface BatchEnabledFacetValues extends FacetValues {
  ImmutableFacetValuesContainer _batchElement();

  ImmutableFacetValuesContainer _common();

  @Override
  BatchEnabledImmutableFacetValues _build();
}

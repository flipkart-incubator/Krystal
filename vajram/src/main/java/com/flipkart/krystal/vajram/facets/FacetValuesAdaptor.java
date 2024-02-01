package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ValueOrError;

@FunctionalInterface
public interface FacetValuesAdaptor {

  /**
   * @return The contents of this request as a map. Missing values are represented by {@link
   *     ValueOrError#empty()}
   */
  Facets toFacetValues();
}

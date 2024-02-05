package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;

@FunctionalInterface
public interface FacetValuesAdaptor {

  /**
   * @return The contents of this request as a map. Missing values are represented by {@link
   *     Errable#empty()}
   */
  Facets toFacetValues();
}

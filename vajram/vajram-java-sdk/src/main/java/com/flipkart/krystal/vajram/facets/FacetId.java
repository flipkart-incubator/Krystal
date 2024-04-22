package com.flipkart.krystal.vajram.facets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
public @interface FacetId {

  /**
   * A facet id is a positive integer which uniquely identifies a facet within the context of a
   * vajram/kryon.
   */
  int value();
}

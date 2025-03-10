package com.flipkart.krystal.vajram.facets;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Target;

@Target({FIELD, TYPE_USE})
public @interface FacetId {

  /**
   * A facet id is a positive integer which uniquely identifies a facet within the context of a
   * vajram/kryon.
   */
  int value();
}

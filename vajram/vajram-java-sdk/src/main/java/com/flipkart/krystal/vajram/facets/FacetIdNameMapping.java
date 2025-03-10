package com.flipkart.krystal.vajram.facets;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

@Target({FIELD, TYPE})
public @interface FacetIdNameMapping {
  int id();

  String name();
}

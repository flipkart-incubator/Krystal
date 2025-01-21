package com.flipkart.krystal.vajram.facets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
public @interface FacetIdNameMapping {
  int id();

  String name();
}

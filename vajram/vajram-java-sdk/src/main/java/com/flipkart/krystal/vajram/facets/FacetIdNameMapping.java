package com.flipkart.krystal.vajram.facets;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(SOURCE)
@Target({FIELD, TYPE})
public @interface FacetIdNameMapping {
  int id();

  String name();
}

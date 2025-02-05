package com.flipkart.krystal.vajram.facets;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Using {
  int value();
}

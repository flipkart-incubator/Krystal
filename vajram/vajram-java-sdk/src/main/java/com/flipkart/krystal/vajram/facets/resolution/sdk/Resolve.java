package com.flipkart.krystal.vajram.facets.resolution.sdk;

import com.flipkart.krystal.facets.FacetType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Resolve {
  String depName();

  String[] depInputs() default {};
}

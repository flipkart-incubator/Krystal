package com.flipkart.krystal.graphql.test;

public @interface EntityFieldMapping {
  String type();

  String[] fieldNames() default {};
}

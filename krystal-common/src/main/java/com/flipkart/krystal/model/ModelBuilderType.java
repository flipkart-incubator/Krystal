package com.flipkart.krystal.model;

public @interface ModelBuilderType {
  Class<? extends ModelBuilder> value() default ModelBuilder.class;
}

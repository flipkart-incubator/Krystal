package com.flipkart.krystal.model;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(CLASS)
public @interface ModelBuilderType {
  Class<? extends ModelBuilder> value() default ModelBuilder.class;
}

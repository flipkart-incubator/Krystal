package com.flipkart.krystal.model;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(CLASS)
@Target(TYPE)
public @interface ModelRoot {
  ModelType type();

  String suffixSeparator() default "_";

  /** If true, then the generated Builder Interface will extend the ModelRoot interface. */
  boolean builderExtendsModelRoot() default false;

  enum ModelType {
    /** This model is a neither a request model nor a response model */
    DEFAULT,
    /** This model is designed to be used as part of requests */
    REQUEST,
    /** This model is designed to be used as part of responses */
    RESPONSE
  }
}

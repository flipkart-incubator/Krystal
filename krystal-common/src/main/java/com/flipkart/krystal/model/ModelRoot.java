package com.flipkart.krystal.model;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(CLASS)
@Target(TYPE)
public @interface ModelRoot {
  ModelType[] type() default {};

  String suffixSeparator() default "_";

  /** If true, then the generated Builder Interface will extend the ModelRoot interface. */
  boolean builderExtendsModelRoot() default false;

  /**
   * If true, all fields in this model must be primitives, boxed primitives, String, a pure Model, a
   * List of these, or a Map with a primitive/String key and a pure Model value. All Model-typed
   * fields must themselves be pure.
   */
  boolean pure() default true;

  enum ModelType {
    /** This model is designed to be used as part of requests */
    REQUEST,
    /** This model is designed to be used as part of responses */
    RESPONSE
  }
}

package com.flipkart.krystal.model;

import static com.flipkart.krystal.model.ModelRoot.ModelType.NONE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(SOURCE)
@Target(ElementType.TYPE)
public @interface ModelRoot {
  ModelType type();

  String suffixSeperator() default "_";

  /** If true, then the generated Builder Interface will extend the ModelRoot interface. */
  boolean builderExtendsModelRoot() default false;

  enum ModelType {
    /** This model is a neither a request model nor a response model */
    NONE,
    /** This model is designed to be used as part of requests */
    REQUEST,
    /** This model is designed to be used as part of responses */
    RESPONSE
  }
}

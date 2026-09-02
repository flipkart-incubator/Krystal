package com.flipkart.krystal.vajram.graphql.client.api;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Binds a GraphQL argument (matched against the schema) to a field on the linked variables model.
 */
@Retention(CLASS)
@Target(METHOD)
@Repeatable(FieldArg.FieldArgs.class)
public @interface FieldArg {

  /** The GraphQL argument name, matched against the schema. */
  String name();

  /** The name of the corresponding field (method) on the variables model. */
  String useVariable();

  @Target(METHOD)
  @interface FieldArgs {
    FieldArg[] value();
  }
}

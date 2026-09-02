package com.flipkart.krystal.vajram.graphql.client.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Declares the relative resource path of the GraphQL SDL schema file that a
 * {@code @GraphQlOpRequest} operation root is validated against.
 */
@Retention(CLASS)
@Target(TYPE)
public @interface GraphQlSchema {
  String path();
}

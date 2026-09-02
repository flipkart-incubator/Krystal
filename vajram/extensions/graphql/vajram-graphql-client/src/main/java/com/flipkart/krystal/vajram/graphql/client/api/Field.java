package com.flipkart.krystal.vajram.graphql.client.api;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Overrides the GraphQL schema field name that a {@code @GraphQlOpRequest}/{@code @GraphQlRequest}
 * model method corresponds to. Defaults to the method name. Protocol-agnostic - decoupled from any
 * JSON/Jackson annotation, since GraphQL response JSON keys by alias (= Java method name), not
 * schema field name.
 */
@Retention(CLASS)
@Target(METHOD)
public @interface Field {
  /** The GraphQL schema field name. Empty string (default) means use the method's own name. */
  String name() default "";
}

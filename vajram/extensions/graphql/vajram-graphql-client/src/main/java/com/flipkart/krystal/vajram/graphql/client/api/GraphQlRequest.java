package com.flipkart.krystal.vajram.graphql.client.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a plain {@code @ModelRoot(type = RESPONSE)} model as a nested selection within a GraphQL
 * operation - the request-facade codegen recurses into such models' methods to build nested
 * selection sets.
 */
@Retention(CLASS)
@Target(TYPE)
public @interface GraphQlRequest {}

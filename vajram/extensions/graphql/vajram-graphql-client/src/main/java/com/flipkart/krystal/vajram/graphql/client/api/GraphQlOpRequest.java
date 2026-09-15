package com.flipkart.krystal.vajram.graphql.client.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a plain {@code @ModelRoot(type = RESPONSE)} model as a GraphQL operation root: its methods
 * are the operation's root selections. Combined with {@code @ForGraphQlOpReq} on a variables model,
 * this triggers generation of a {@code <OperationRoot>QueryFacade} class.
 */
@Retention(CLASS)
@Target(TYPE)
public @interface GraphQlOpRequest {

  /** Relative resource path of the GraphQL SDL schema file to validate this operation against. */
  String schemaFilePath();
}

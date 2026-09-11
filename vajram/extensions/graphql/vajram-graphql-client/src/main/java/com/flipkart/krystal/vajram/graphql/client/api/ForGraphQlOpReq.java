package com.flipkart.krystal.vajram.graphql.client.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a plain {@code @ModelRoot(type = REQUEST)} model as the variables model for the given
 * {@code @GraphQlOpRequest} operation root. This is the trigger annotation for {@code
 * <OperationRoot>QueryFacade} generation: it gives the facade's {@code of(...)} parameter its
 * concrete, type-safe parameter type directly.
 */
@Retention(CLASS)
@Target(TYPE)
public @interface ForGraphQlOpReq {
  Class<?> value();
}

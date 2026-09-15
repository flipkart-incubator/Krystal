package com.flipkart.krystal.vajram.graphql.client.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a {@code @GraphQlRequest} model as a reusable GraphQL fragment, and marks a supertype
 * reference in another {@code @GraphQlRequest}/{@code @GraphQlOpRequest} model's {@code extends}
 * clause as a spread of that fragment.
 *
 * <p>Both declarations are required for a fragment to be spread - the fragment interface's own
 * declaration, and the type-use annotation on the reference in the child's {@code extends} clause:
 *
 * <pre>{@code
 * @GraphQlFragment
 * @GraphQlRequest
 * @ModelRoot(type = RESPONSE)
 * @SupportedModelProtocol(Json.class)
 * public interface ProductFragment extends Model {
 *   String id();
 *   String name();
 * }
 *
 * @GraphQlRequest
 * @ModelRoot(type = RESPONSE)
 * @SupportedModelProtocol(Json.class)
 * public interface ProductReq extends @GraphQlFragment ProductFragment {
 *   String sku(); // fields declared directly here are still inlined alongside the spread
 * }
 * }</pre>
 *
 * <p>The request-facade codegen emits a top-level {@code fragment ProductFragment on Product { id
 * name }} definition and a {@code ...ProductFragment} spread in place of inlining the fragment's
 * fields. A supertype reference annotated on only one of the two sites is a compile error.
 */
@Retention(CLASS)
@Target({TYPE, TYPE_USE})
public @interface GraphQlFragment {
  /** GraphQL fragment name. Empty string (default) means use the interface's simple name. */
  String name() default "";
}

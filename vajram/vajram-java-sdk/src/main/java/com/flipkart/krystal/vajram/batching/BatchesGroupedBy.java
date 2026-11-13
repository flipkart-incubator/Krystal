package com.flipkart.krystal.vajram.batching;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a given facet (input or injection) of a vajram which supports batching as a <i>batch
 * key</i>: the {@link InputBatcher} places two requests in the same batch only if the values of all
 * {@code @BatchesGroupedBy} facets of the requests are equal. The value of a batch key is read
 * individually in the output logic. A facet cannot be both {@link Batched} and
 * {@code @BatchesGroupedBy}.
 *
 * <p>This annotation is not allowed on facets which are computed by the vajram - dependencies,
 * fields and outputs (a compile error). In particular, dependency responses cannot be batch keys as
 * they do not have value based equality: they contain the request that was sent to the dependency.
 * A given facet is either a batch key or {@link Batched}, which is why only given facets need to be
 * annotated.
 *
 * <p>Batching rules:
 *
 * <ol>
 *   <li>{@link Batched} can be placed on any facet. Such facets are read only via the batch items.
 *   <li>{@link BatchesGroupedBy} can be placed only on given facets (inputs and injections).
 *   <li>Facets which are not {@link Batched} cannot depend, directly or indirectly, on {@link
 *       Batched} facets.
 *   <li>Facets which are not {@link Batched} are always accessed individually in the output logic
 *       (even inputs).
 *   <li>All given facets of a vajram which supports batching must have either {@link Batched} or
 *       {@link BatchesGroupedBy}.
 * </ol>
 *
 * <p>Together these guarantee that all facets which are not {@link Batched} have the same value for
 * all the requests in a batch: the given ones because they are batch keys, and the rest because
 * they are derived only from batch keys. Rules 2 and 5 and the facets which can be validated from
 * {@code @Resolve} and {@code @ComputeField} methods in rule 3 are validated at compile time. All
 * the rules are validated again when the vajram graph is loaded (which also covers resolvers
 * defined via {@code getSimpleInputResolvers()}).
 */
@Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface BatchesGroupedBy {}

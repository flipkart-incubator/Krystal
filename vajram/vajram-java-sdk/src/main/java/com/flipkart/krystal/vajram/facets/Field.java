package com.flipkart.krystal.vajram.facets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in the {@code _InternalFacets} interface as a field facet.
 *
 * <p>Field facets are computed locally by this vajram using a {@code @ComputeField} annotated
 * static method. They can consume any other facets (inputs, injections, dependencies, or other
 * fields) and can be consumed by resolvers, output logic, and other field computations.
 *
 * <p>Fields are optional by default. Use {@code @IfAbsent} to make a field mandatory.
 *
 * <p>Fields must be declared with plain types (e.g., {@code String}), never with wrapper types
 * (e.g., {@code Errable<String>} or {@code Optional<String>}). When consuming optional fields in
 * logic methods, use {@code @Nullable}, {@code Optional<>}, or {@code Errable<>} parameter types.
 *
 * <p>Example:
 *
 * <pre>{@code
 * interface _InternalFacets {
 *   @Field
 *   String fullName();  // Optional by default
 *
 *   @Field
 *   @IfAbsent(FAIL)
 *   String normalizedName();  // Mandatory field
 * }
 *
 * @ComputeField(fullName_n)
 * static String computeFullName(String firstName, String lastName) {
 *   return firstName + " " + lastName;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Field {}

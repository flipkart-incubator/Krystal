package com.flipkart.krystal.vajram.facets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static method as the computation logic for a field facet.
 *
 * <p>The annotated method must be static and can consume any other facets (inputs, injections,
 * dependencies, or other fields) as parameters. The method's return type must match the field's
 * declared type.
 *
 * <p>Like resolvers, {@code @ComputeField} methods must be pure computations and cannot make IO
 * calls. A field is computed as soon as all the facets which its compute method consumes are ready.
 *
 * <p>Example:
 *
 * <pre>{@code
 * interface _InternalFacets {
 *   @Field String fullName();
 *   @Field String greeting();
 * }
 *
 * @ComputeField(fullName_n)
 * static String computeFullName(String firstName, String lastName) {
 *   return firstName + " " + lastName;
 * }
 *
 * @ComputeField(greeting_n)
 * static String computeGreeting(String fullName) {
 *   return "Hello, " + fullName;
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ComputeField {
  /**
   * The qualified facet name of the field being computed.
   *
   * <p>A qualified facet name looks like {@code "CurrentVajramId:fieldName"}. Developers should not
   * pass hand-written strings here. The Vajram SDK code generator generates the qualified names for
   * all facets in the vajram facets interface ({@code VajramId_Fac}) as static constants named as
   * {@code fieldName_n}. These auto-generated string constants should be used for this parameter to
   * avoid manual errors.
   *
   * <p>Example: {@code @ComputeField(fullName_n)} where {@code fullName_n} is the auto-generated
   * constant for the field.
   */
  String value();
}

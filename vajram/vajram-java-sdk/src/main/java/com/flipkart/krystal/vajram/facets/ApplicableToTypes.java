package com.flipkart.krystal.vajram.facets;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Used to specify the types to which a given feature is applicable. */
@Retention(CLASS)
@Target(FIELD)
public @interface ApplicableToTypes {
  /**
   * The types to which the feature is applicable. If this is set to an empty array, and {@link
   * #all()} is set to true, then this feature is applicable to all types. If this is set to an
   * empty and {@link #all()} is set to false, then this feature is applicable to no types.
   */
  Class<?>[] value() default {};

  /**
   * Whether this feature is applicable to all types. The value of this parameter is respected only
   * if {@link #value()} is an empty array.
   */
  boolean all() default false;
}

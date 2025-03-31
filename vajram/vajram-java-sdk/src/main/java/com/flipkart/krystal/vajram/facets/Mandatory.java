package com.flipkart.krystal.vajram.facets;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.tags.ElementTags;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

/** A facet {@link ElementTags tagged} with this annotation is considered mandatory. */
@Retention(RUNTIME)
@Target(FIELD)
@Documented
public @interface Mandatory {

  IfNotSet ifNotSet() default IfNotSet.FAIL;

  /**
   * Specify the condition under which the facet is mandatory. This must be set if only if {@link
   * #ifNotSet()} is set to {@link IfNotSet#MAY_FAIL_CONDITIONALLY}. In all other cases, this value
   * is auto-inferred as "ALWAYS" or "NEVER" depending on the value of {@link #ifNotSet()}.
   */
  String mandatoryWhen() default "";

  /** The behavior to follow if the facet value is not set. */
  enum IfNotSet {
    /** The vajram should fail if the facet value is not set. This is the default behavior. */
    @ApplicableToTypes(all = true)
    FAIL(false),

    /**
     * Specifies that the facet is mandatory but only in specific conditions - in other conditions,
     * its optional. This is different from the cases where the facet is always Optional(depicted by
     * omitting this annotation) or the facet is strictly mandatory meaning missing value value will
     * instantly fail the vajram execution (depicted by using @Mandatory(ifNotSet = FAIL) which is
     * the same as @Mandatory).
     */
    @ApplicableToTypes(all = true)
    MAY_FAIL_CONDITIONALLY(false),

    /**
     * The vajram should use the default value "false" if a mandatory boolean facet value is not
     * set.
     */
    @ApplicableToTypes({Boolean.class, boolean.class})
    DEFAULT_TO_FALSE(true),

    /**
     * The vajram should use the default value "0" if a mandatory numeric (byte, short, int, long,
     * float, double) facet value is not set.
     */
    @ApplicableToTypes({
      Byte.class, byte.class,
      Short.class, short.class,
      Integer.class, int.class,
      Long.class, long.class,
      Float.class, float.class,
      Double.class, double.class
    })
    DEFAULT_TO_ZERO(true),

    /**
     * The vajram should use the default value "empty" of the relevant type if a mandatory facet
     * value whose type is ahy of array, collection, map, or strings, is unset.
     */
    @ApplicableToTypes({String.class, Array.class, List.class, Map.class})
    DEFAULT_TO_EMPTY(true);

    private final boolean usePlatformDefault;

    IfNotSet(boolean usePlatformDefault) {
      this.usePlatformDefault = usePlatformDefault;
    }

    /**
     * Returns true if the platform default value should be used for the facet with this ifNotSet
     * strategy.
     */
    public boolean usePlatformDefault() {
      return usePlatformDefault;
    }
  }
}

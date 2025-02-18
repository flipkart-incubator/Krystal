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

  /** The behavior to follow if the facet value is not set. */
  public static enum IfNotSet {
    /** The vajram should fail if the facet value is not set. This is the default behavior. */
    @ApplicableToTypes(all = true)
    FAIL(false),

    /**
     * The vajram should use the default value "false" if a mandatory boolean facet value is not
     * set.
     */
    @ApplicableToTypes(Boolean.class)
    DEFAULT_TO_FALSE(true),

    /**
     * The vajram should use the default value "0" if a mandatory numeric (byte, short, int, long,
     * float, double) facet value is not set.
     */
    @ApplicableToTypes({
      Byte.class,
      Short.class,
      Integer.class,
      Long.class,
      Float.class,
      Double.class
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

    public boolean usePlatformDefault() {
      return usePlatformDefault;
    }
  }
}

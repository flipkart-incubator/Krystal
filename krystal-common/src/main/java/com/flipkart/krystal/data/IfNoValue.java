package com.flipkart.krystal.data;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Facet;
import com.flipkart.krystal.datatypes.ApplicableToTypes;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.tags.ElementTags;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

/**
 * This annotation documents the strategy to follow when a facet has no value. If a facet isn't
 * {@link ElementTags tagged} this annotation, then the vajram is contractually obligated to handle
 * ALL CASES in which the facet has no value - in other words, the facet having no value can never
 * cause a failure in the vajram execution.
 */
@Retention(RUNTIME)
@Target({FIELD, METHOD})
@ApplicableToElements(Facet.class)
@Documented
public @interface IfNoValue {

  Strategy then() default Strategy.FAIL;

  /**
   * Specify the condition under which the facet is mandatory. This must be set if only if {@link
   * #then()} is set to {@link Strategy#MAY_FAIL_CONDITIONALLY}. In all other cases, this value is
   * auto-inferred as "ALWAYS" or "NEVER" depending on the value of {@link #then()}.
   */
  String conditionalFailureInfo() default "";

  /** The behavior to follow if the facet value is not set. */
  enum Strategy {
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
    @ApplicableToTypes(boolean.class)
    DEFAULT_TO_FALSE(true),

    /**
     * The vajram should use the default value "0" if a mandatory numeric (byte, short, int, long,
     * float, double) facet value is not set.
     */
    @ApplicableToTypes({byte.class, short.class, int.class, long.class, float.class, double.class})
    DEFAULT_TO_ZERO(true),

    /**
     * The vajram should use the default value "empty" of the relevant type if a mandatory facet
     * value whose type is ahy of array, collection, map, or strings, is unset.
     */
    @ApplicableToTypes({String.class, Array.class, List.class, Map.class})
    DEFAULT_TO_EMPTY(true),

    /** */
    @ApplicableToTypes(Model.class)
    DEFAULT_TO_MODEL_DEFAULTS(true);

    private final boolean usePlatformDefault;

    Strategy(boolean usePlatformDefault) {
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

package com.flipkart.krystal.data;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Facet;
import com.flipkart.krystal.datatypes.ApplicableToTypes;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.tags.ElementTags;
import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.UtilityClass;

/**
 * This annotation documents the strategy to follow when a data element in a request - like an input
 * facet or field in a request model has no value (i.e. {@code null} or {@link Nil}). If a data
 * element isn't {@link ElementTags tagged} with this annotation, then the platform will infer a
 * default value based on the context. For example, in case of input facets of vajrams, the Krystal
 * platform defaults to {@code @IfNoValue(then=WILL_NEVER_FAIL)} in case this annotaiton is not
 * used.
 *
 * <p>In case of maps and lists, empty maps and empty lists are considered as valid values, so this
 * annotation doesn't affect them.
 */
@Retention(RUNTIME)
@Target({FIELD, METHOD})
@ApplicableToElements(Facet.class)
@Documented
public @interface IfAbsent {

  IfAbsentThen value();

  /**
   * Specify the condition under which the facet is mandatory. This must be set if only if {@link
   * #value()} is set to {@link IfAbsentThen#MAY_FAIL_CONDITIONALLY}. In all other cases, this value
   * is auto-inferred as "ALWAYS" or "NEVER" depending on the value of {@link #value()}.
   */
  String conditionalFailureInfo() default "";

  /** The behavior to follow if the facet value is not set. */
  enum IfAbsentThen {

    /**
     * The author of the code guarantees that the code will never fail because of this value not
     * being set. For example, the code might always default to some value when there is no value.
     */
    @ApplicableToTypes(all = true)
    WILL_NEVER_FAIL(false, false),

    /**
     * Specifies that the facet is mandatory but only in specific conditions - otherwise, it's
     * optional. In other words, the code author declares that if a value is not provided, the code
     * might fail in specific conditions.
     *
     * <p>This is different from the cases where the facet is always Optional(depicted by {@link
     * #WILL_NEVER_FAIL}) or the facet is strictly mandatory meaning missing value will always fail
     * (depicted by using {@link #FAIL}).
     *
     * <p>If IfNoValue annotation is missing, then this is considered the default.
     */
    @ApplicableToTypes(all = true)
    MAY_FAIL_CONDITIONALLY(false, false),

    /**
     * The application will fail if the value is not set. In colloquial terms, it is said that the
     * value is "mandatory"
     */
    @ApplicableToTypes(all = true)
    FAIL(false, true),

    /**
     * The vajram should use the default value "false" if a mandatory boolean facet value is not
     * set.
     */
    @ApplicableToTypes(boolean.class)
    DEFAULT_TO_FALSE(true, true),

    /**
     * The vajram should use the default value "0" if a mandatory numeric (byte, short, int, long,
     * float, double) facet value is not set.
     */
    @ApplicableToTypes({byte.class, short.class, int.class, long.class, float.class, double.class})
    DEFAULT_TO_ZERO(true, true),

    /**
     * The vajram should use the default value "empty" of the relevant type if a mandatory facet
     * value whose type is ahy of array, collection, map, or strings, is unset.
     */
    @ApplicableToTypes({String.class, Array.class, List.class, Map.class})
    DEFAULT_TO_EMPTY(true, true),

    @ApplicableToTypes(Model.class)
    DEFAULT_TO_MODEL_DEFAULTS(true, true);

    /**
     * true if the platform default value should be used for the facet with this ifNotSet strategy.
     */
    @Getter private final boolean usePlatformDefault;

    @Getter private final boolean isMandatoryOnServer;

    IfAbsentThen(boolean usePlatformDefault, boolean isMandatoryOnServer) {
      this.usePlatformDefault = usePlatformDefault;
      this.isMandatoryOnServer = isMandatoryOnServer;
    }
  }

  @UtilityClass
  final class Creator {
    public static @AutoAnnotation IfAbsent create(
        IfAbsentThen value, String conditionalFailureInfo) {
      return new AutoAnnotation_IfAbsent_Creator_create(value, conditionalFailureInfo);
    }

    public static IfAbsent createDefault() {
      return create(IfAbsentThen.WILL_NEVER_FAIL, "");
    }
  }
}

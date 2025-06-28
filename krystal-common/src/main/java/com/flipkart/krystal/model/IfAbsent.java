package com.flipkart.krystal.model;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Facet;
import com.flipkart.krystal.data.Nil;
import com.flipkart.krystal.tags.ElementTags;
import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import lombok.Getter;
import lombok.experimental.UtilityClass;

/**
 * This annotation documents the strategy to follow when a data element in a request - like an input
 * facet or field in a request model has no value (i.e. {@code null} or {@link Nil}). If a data
 * element isn't {@link ElementTags tagged} with this annotation, then the platform will infer a
 * default value based on the context. For example, in case of input facets of vajrams, the Krystal
 * platform defaults to {@code @IfAbsent(WILL_NEVER_FAIL)} in case this annotation is not used.
 *
 * <p>The interpretation of this annotation is context specific. For example, when placed on a field
 * in a request model or an input facet of a vajram, it applies to the cases when the client who
 * sent the request did not send any value (as discussed above, empty collections and maps are
 * considered to be "present" not "absent". When placed on a dependency, it means: the dependency
 * was not called, or the dependency was called N times (If N>1, we call it a fanout dependency) -
 * and all N calls failed.
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
     * <p>If IfAbsent annotation is missing, then this is considered the default.
     */
    MAY_FAIL_CONDITIONALLY(false, false),

    /**
     * The application will fail if the value is not set. In colloquial terms, it is said that the
     * value is "mandatory"
     */
    FAIL(false, true),

    /**
     * If the data field is not set, then assume that a default value has been set. The default
     * value is data type specific standard and is generally something which amounts to a
     * "zero-like" or "empty" or "false" value.
     *
     * <p>This is an advanced option designed to allow interoperability with some serialization
     * protocols like protobuf which allow developers to opt in to the behaviour where missing
     * values are automatically assumed to have default values which unlocks better serialization
     * performance (since nulls and default values do not need to be serialized).
     *
     * <p>Examples of default values:
     *
     * <ul>
     *   <li>numbers (char, byte, short, int, long, double, float): 0
     *   <li>string: empty string
     *   <li>collections and arrays: empty collection/array
     *   <li>map: empty map
     *   <li>boolean: false
     * </ul>
     */
    ASSUME_DEFAULT_VALUE(true, true);

    private static final EnumSet<IfAbsentThen> ALWAYS_OPTIONAL_FOR_CLIENTS =
        EnumSet.of(WILL_NEVER_FAIL, ASSUME_DEFAULT_VALUE);

    private static final EnumSet<IfAbsentThen> ALWAYS_MANDATORY_ON_SERVER =
        EnumSet.of(FAIL, ASSUME_DEFAULT_VALUE);

    /**
     * true if the platform default value should be used for the facet with this ifNotSet strategy.
     */
    @Getter private final boolean usePlatformDefault;

    @Getter private final boolean isMandatoryOnServer;

    IfAbsentThen(boolean usePlatformDefault, boolean isMandatoryOnServer) {
      this.usePlatformDefault = usePlatformDefault;
      this.isMandatoryOnServer = isMandatoryOnServer;
    }

    public boolean isMandatoryOnServer() {
      return ALWAYS_MANDATORY_ON_SERVER.contains(this);
    }

    public boolean isOptionalForClient() {
      return ALWAYS_OPTIONAL_FOR_CLIENTS.contains(this);
    }
  }

  @UtilityClass
  final class Creator {
    public static @AutoAnnotation IfAbsent create(
        IfAbsentThen value, String conditionalFailureInfo) {
      return new AutoAnnotation_IfAbsent_Creator_create(value, conditionalFailureInfo);
    }
  }
}

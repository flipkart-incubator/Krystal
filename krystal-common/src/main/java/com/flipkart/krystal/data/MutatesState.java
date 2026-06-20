package com.flipkart.krystal.data;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.annos.ElementTagUtility;
import com.flipkart.krystal.annos.Transitive;
import com.flipkart.krystal.core.KrystalElement.Vajram;
import com.flipkart.krystal.datatypes.Trilean;
import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import lombok.experimental.UtilityClass;

/**
 * Indicates whether the vajram mutates some state of a system or not. The state of the system
 * includes both in-memory state local to the current process or external state such as databases or
 * distributed caches.
 *
 * <p>This annotation represents a transitive property of a vajram. i.e. this property of a vajram
 * can impact the value of its client vajrams
 */
@Target(TYPE)
@Retention(RUNTIME)
@ApplicableToElements(Vajram.class)
@ElementTagUtility(MutatesStates.class)
@Transitive
public @interface MutatesState {

  /**
   *
   *
   * <ul>
   *   <li>TRUE - This vajram has logic which can mutate the state of the system.
   *   <li>FALSE - This vajram can never mutate state of the system.
   *   <li>UNKNOWN - It is unknown whether this vajram mutates state of the system or not. This is
   *       the value assumed if this annotation is not present or induced onto a vajram. This is
   *       also used when a vajram can dynamically decide whether to mutate the state or not (For
   *       example, the decision depends on the value of an input or some injected value)
   * </ul>
   */
  Trilean value();

  /**
   * The entity's/entities' namespaced names whose data that this vajram handles.
   *
   * <ul>
   *   <li>If {@link #value()} is TRUE, this denotes the entity/entities whose state this vajram
   *       mutates.
   *   <li>If {@link #value()} is FALSE, this denotes the entity/entities whose state this vajram
   *       reads.
   *   <li>If {@link #value()} is UNKNOWN, this denotes the entity/entities whose state this vajram
   *       could read or mutate.
   * </ul>
   */
  String[] entities() default {};

  @UtilityClass
  final class Creator {
    @AutoAnnotation
    public static MutatesState create(Trilean value) {
      return new AutoAnnotation_MutatesState_Creator_create(value);
    }
  }
}

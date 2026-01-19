package com.flipkart.krystal.core;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Vajram;
import com.flipkart.krystal.datatypes.Trilean;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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
public @interface MutatesState {

  /**
   *
   *
   * <ul>
   *   <li>TRUE - This vajram has logic which can mutate the state of the system.
   *   <li>FALSE - This vajram can never mutate state of the system.
   *   <li>UNKNOWN - It is unknown whether this vajram mutates state of the system or not. This is
   *       the default value if this annotation is absent or not inferrable from dependencies. The
   *       interpretation of this depends on the application. Depending on the application's use
   *       case, it might choose to interpret this as TRUE or FALSE. In general, it is considered
   *       safer to interpret this as TRUE since vajrams which mutate state can have side effects
   *       which can impact the correctness of the overall computation (for example race conditions,
   *       etc).
   * </ul>
   */
  Trilean value();
}

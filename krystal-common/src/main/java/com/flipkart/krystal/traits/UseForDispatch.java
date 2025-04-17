package com.flipkart.krystal.traits;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Facet;
import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Only trait inputs annotated as UseForDispatch can be used for <a
 * href="https://en.wikipedia.org/wiki/Dynamic_dispatch">dynamic dispatching</a> trait invocations
 * to conforming vajrams
 *
 * @see PredicateDynamicDispatchPolicy
 */
@ApplicableToElements(Facet.class)
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface UseForDispatch {
  public static class Creator {
    public static @AutoAnnotation UseForDispatch create() {
      return new AutoAnnotation_UseForDispatch_Creator_create();
    }

    private Creator() {}
  }
}

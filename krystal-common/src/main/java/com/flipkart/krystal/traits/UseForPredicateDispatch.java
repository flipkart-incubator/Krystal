package com.flipkart.krystal.traits;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Facet;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Only trait inputs annotated with this annotation can be used for <a
 * href="https://en.wikipedia.org/wiki/Dynamic_dispatch">dynamic dispatching</a> trait invocations
 * to conforming vajrams
 *
 * @see PredicateDispatchPolicy
 */
@ApplicableToElements(Facet.class)
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface UseForPredicateDispatch {}

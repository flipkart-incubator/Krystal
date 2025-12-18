package com.flipkart.krystal.lattice.core.di;

import static java.lang.annotation.ElementType.METHOD;

import com.flipkart.krystal.lattice.core.doping.Dopant;
import java.lang.annotation.Annotation;
import java.lang.annotation.Target;

/**
 * Methods in {@link Dopant} classes annotated with this are used by the code generators to
 * generated bindings in the dependency injection framework native to the lattice application.
 *
 * <p>This is intended to be framework-neutral version of {@link jakarta.enterprise.inject.Produces}
 */
@Target(METHOD)
public @interface Produces {
  Class<? extends Annotation> inScope() default NoScope.class;

  @interface NoScope {}
}

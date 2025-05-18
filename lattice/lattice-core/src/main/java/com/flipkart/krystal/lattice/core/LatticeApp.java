package com.flipkart.krystal.lattice.core;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

@Target(TYPE)
public @interface LatticeApp {
  Class<? extends DependencyInjectionBinder> dependencyInjectionBinder() default
      DependencyInjectionBinder.class;
}

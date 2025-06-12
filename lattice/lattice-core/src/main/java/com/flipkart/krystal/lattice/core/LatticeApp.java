package com.flipkart.krystal.lattice.core;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder;
import java.lang.annotation.Target;

@Target(TYPE)
public @interface LatticeApp {
  String description();

  Class<? extends DependencyInjectionBinder> dependencyInjectionBinder() default
      DependencyInjectionBinder.class;
}

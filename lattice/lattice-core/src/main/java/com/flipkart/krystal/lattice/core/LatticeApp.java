package com.flipkart.krystal.lattice.core;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.lattice.core.di.DependencyInjectionProvider;
import java.lang.annotation.Target;

@Target(TYPE)
public @interface LatticeApp {
  String description();

  Class<? extends DependencyInjectionProvider> dependencyInjectionBinder() default
      DependencyInjectionProvider.class;
}

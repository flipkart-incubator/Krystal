package com.flipkart.krystal.lattice.ext.guice;

import com.flipkart.krystal.lattice.core.di.DependencyInjector;
import com.flipkart.krystal.vajram.guice.inputinjection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.google.inject.Guice;
import com.google.inject.Injector;

public record GuiceInjector(Injector injector) implements DependencyInjector {

  @Override
  public <T> T getInstance(Class<T> clazz) {
    return injector.getInstance(clazz);
  }
}

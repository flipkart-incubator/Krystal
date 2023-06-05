package com.flipkart.krystal.vajram.samples;

import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjectionProvider;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

public final class GuiceDIProvider implements InputInjectionProvider {

  private final Injector injector;

  public GuiceDIProvider(Injector injector) {
    this.injector = injector;
  }

  public Object getInstance(Class<?> clazz) {
    return injector.getInstance(clazz);
  }

  public Object getInstance(Class<?> clazz, String injectionName) {
    return injector.getInstance(Key.get(clazz, Names.named(injectionName)));
  }
}

package com.flipkart.krystal.vajram.samples;

import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjectionProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.util.HashSet;
import java.util.Set;

public final class GuiceDIProvider implements InputInjectionProvider {

  private final Injector injector;

  private GuiceDIProvider(Set<AbstractModule> modules) {
    injector = Guice.createInjector(modules);
  }

  public Object getInstance(Class<?> clazz) {

    return injector.getInstance(clazz);
  }

  public Object getInstance(Class<?> clazz, String injectionName) {
    return injector.getInstance(Key.get(clazz, Names.named(injectionName)));
  }

  public static final class Builder {
    private final Set<AbstractModule> modules = new HashSet<>();

    public Builder loadFromModule(AbstractModule module) {
      modules.add(module);
      return this;
    }

    public GuiceDIProvider build() {
      return new GuiceDIProvider(this.modules);
    }
  }
}

package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.adaptors.DependencyInjectionAdaptor;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.util.HashSet;
import java.util.Set;

public final class GuiceDIAdaptor implements DependencyInjectionAdaptor {

  private final Injector injector;

  private GuiceDIAdaptor(Set<AbstractModule> modules) {
    injector = Guice.createInjector(modules);
  }

  public Object getInstance(Class<?> clazz) {

    return injector.getInstance(clazz);
  }

  public Object getInstance(Class<?> clazz, String annotation) {
    return injector.getInstance(Key.get(clazz, Names.named(annotation)));
  }

  public static final class Builder {
    private final Set<AbstractModule> modules = new HashSet<>();

    public Builder loadFromModule(AbstractModule module) {
      modules.add(module);
      return this;
    }

    public GuiceDIAdaptor build() {
      return new GuiceDIAdaptor(this.modules);
    }
  }
}

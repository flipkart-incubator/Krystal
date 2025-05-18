package com.flipkart.krystal.lattice.ext.guice;

import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategy;
import com.flipkart.krystal.vajram.guice.inputinjection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;
import jakarta.inject.Singleton;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class GuiceModuleBinder implements DependencyInjectionBinder {

  private final List<Binding<?>> bindings = new ArrayList<>();
  private final List<Class<?>> singletons = new ArrayList<>();
  private final AbstractModule rootModule;
  private @MonotonicNonNull com.flipkart.krystal.lattice.ext.guice.GuiceInjector guiceInjector;
  private @MonotonicNonNull VajramGuiceInputInjector vajramGuiceInputInjector;

  public GuiceModuleBinder(Module... modules) {
    this(ImmutableList.copyOf(modules));
  }

  public GuiceModuleBinder(ImmutableList<? extends Module> modules) {
    this.rootModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            modules.forEach(this::install);
            bindings.forEach(binding -> binding.bind(binder()));
            singletons.forEach(c -> bind(c).in(Singleton.class));
          }
        };
  }

  @Override
  public <T> void bindToInstance(Class<T> type, T instance) {
    bindings.add(new Binding<>(type, instance));
  }

  @Override
  public <T> void bindInSingleton(Class<T> type) {
    singletons.add(type);
  }

  @Override
  public GuiceInjector getInjector() {
    GuiceInjector guiceInjector = this.guiceInjector;
    if (guiceInjector == null) {
      guiceInjector = new GuiceInjector(Guice.createInjector(getRootModule()));
      this.guiceInjector = guiceInjector;
    }
    return guiceInjector;
  }

  public Module getRootModule() {
    return rootModule;
  }

  @Override
  public VajramInjectionProvider toVajramInjectionProvider() {
    if (vajramGuiceInputInjector == null) {
      vajramGuiceInputInjector = new VajramGuiceInputInjector(getInjector().injector());
    }
    return vajramGuiceInputInjector;
  }

  @Override
  public Closeable openRequestScope(
      Map<Object, Object> seedMap, ThreadingStrategy threadingStrategy) {
    ;
  }

  private record Binding<T>(Class<T> type, T instance) {
    void bind(Binder binder) {
      binder.bind(type).toInstance(instance);
    }
  }
}

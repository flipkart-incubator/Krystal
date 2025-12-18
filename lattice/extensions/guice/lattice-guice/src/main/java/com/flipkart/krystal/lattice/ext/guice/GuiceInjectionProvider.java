package com.flipkart.krystal.lattice.ext.guice;

import com.flipkart.krystal.lattice.core.LatticeApplication;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionProvider;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategy;
import com.flipkart.krystal.vajram.guice.injection.VajramGuiceInputInjector;
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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class GuiceInjectionProvider implements DependencyInjectionProvider {

  private final List<InstanceBinding<?>> instanceBindings = new ArrayList<>();
  private final List<TypeBinding<?>> typeBindings = new ArrayList<>();
  private final List<Class<?>> singletons = new ArrayList<>();
  private @MonotonicNonNull AbstractModule rootModule;
  private @MonotonicNonNull GuiceValueProvider guiceValueProvider;
  private @MonotonicNonNull VajramGuiceInputInjector vajramGuiceInputInjector;
  private final LatticeApplication latticeApp;
  private final ImmutableList<? extends Module> modules;

  public GuiceInjectionProvider(LatticeApplication latticeApp, Module... modules) {
    this(latticeApp, ImmutableList.copyOf(modules));
  }

  private GuiceInjectionProvider(
      LatticeApplication latticeApp, ImmutableList<? extends Module> modules) {
    this.latticeApp = latticeApp;
    this.modules = modules;
  }

  protected List<Module> getExtensionModules() {
    return List.of();
  }

  @Override
  public <T> void bindToInstance(Class<? extends T> type, T instance) {
    //noinspection unchecked
    instanceBindings.add(new InstanceBinding<>((Class<T>) type, instance));
  }

  @Override
  public <T> void bindInSingleton(Class<T> type) {
    singletons.add(type);
  }

  @Override
  public <T> void bind(Class<T> type, Class<? extends T> to) {
    typeBindings.add(new TypeBinding<>(type, to));
  }

  @Override
  public GuiceValueProvider getValueProvider() {
    GuiceValueProvider guiceValueProvider = this.guiceValueProvider;
    if (guiceValueProvider == null) {
      guiceValueProvider =
          new GuiceValueProvider(
              Guice.createInjector(
                  getRootModule(),
                  new AbstractModule() {
                    @Override
                    protected void configure() {
                      binder().bind(LatticeApplication.class).toInstance(latticeApp);
                      binder()
                          .bind(DependencyInjectionProvider.class)
                          .toInstance(GuiceInjectionProvider.this);
                    }
                  }));
      this.guiceValueProvider = guiceValueProvider;
    }
    return guiceValueProvider;
  }

  public Module getRootModule() {
    if (rootModule == null) {
      this.rootModule =
          new AbstractModule() {
            @Override
            protected void configure() {
              getExtensionModules().forEach(this::install);
              modules.forEach(this::install);
              instanceBindings.forEach(instanceBinding -> instanceBinding.bind(binder()));
              typeBindings.forEach(typeBinding -> typeBinding.bind(binder()));
              singletons.forEach(c -> bind(c).in(Singleton.class));
            }
          };
    }
    return rootModule;
  }

  @Override
  public VajramInjectionProvider toVajramInjectionProvider() {
    if (vajramGuiceInputInjector == null) {
      vajramGuiceInputInjector = new VajramGuiceInputInjector(getValueProvider().injector());
    }
    return vajramGuiceInputInjector;
  }

  @Override
  public Closeable openRequestScope(Bindings seedMap, ThreadingStrategy threadingStrategy) {
    return () -> {};
  }

  private record InstanceBinding<T>(Class<T> type, T instance) {
    void bind(Binder binder) {
      binder.bind(type).toInstance(instance);
    }
  }

  private record TypeBinding<T>(Class<T> type, Class<? extends T> to) {
    void bind(Binder binder) {
      binder.bind(type).to(to);
    }
  }
}

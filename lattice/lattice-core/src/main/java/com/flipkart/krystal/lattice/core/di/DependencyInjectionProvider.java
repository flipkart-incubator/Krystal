package com.flipkart.krystal.lattice.core.di;

import com.flipkart.krystal.lattice.core.execution.ThreadingStrategy;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import java.io.Closeable;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DependencyInjectionProvider {
  <T> void bindToInstance(Class<? extends T> type, T instance);

  <T> void bindInSingleton(Class<T> type);

  <T> void bind(Class<T> type, Class<? extends T> to);

  InjectionValueProvider getInjector();

  default @Nullable VajramInjectionProvider toVajramInjectionProvider() {
    return null;
  }

  Closeable openRequestScope(Bindings seedMap, ThreadingStrategy threadingStrategy);
}

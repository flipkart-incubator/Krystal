package com.flipkart.krystal.lattice.core.di;

import com.flipkart.krystal.lattice.core.execution.ThreadingStrategy;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import java.io.Closeable;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DependencyInjectionBinder {
  <T> void bindToInstance(Class<T> type, T instance);

  <T> void bindInSingleton(Class<T> type);

  DependencyInjector getInjector();

  default @Nullable VajramInjectionProvider toVajramInjectionProvider() {
    return null;
  }

  Closeable openRequestScope(Bindings seedMap, ThreadingStrategy threadingStrategy);
}

package com.flipkart.krystal.lattice.core.di;

import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DependencyInjector {
  <T> T getInstance(Class<T> clazz);
}

package com.flipkart.krystal.lattice.core.di;

public interface DependencyInjector {
  <T> T getInstance(Class<T> clazz);
}

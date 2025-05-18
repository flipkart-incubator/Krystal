package com.flipkart.krystal.lattice.core;

public interface DependencyInjectionBinder {
  <T> void bindToInstance(Class<T> type, T instance);

  <T> void bindInSingleton(Class<T> type);

  DependencyInjector createInjector();
}

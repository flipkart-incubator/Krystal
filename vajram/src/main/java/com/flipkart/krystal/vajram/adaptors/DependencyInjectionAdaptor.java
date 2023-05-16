package com.flipkart.krystal.vajram.adaptors;

public interface DependencyInjectionAdaptor<T> {
  T getInjector();

  Object getInstance(Class<?> clazz);

  Object getInstance(Class<?> clazz, String annotation);
}

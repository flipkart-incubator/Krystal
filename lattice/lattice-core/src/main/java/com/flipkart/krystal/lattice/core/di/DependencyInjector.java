package com.flipkart.krystal.lattice.core;

public interface DependencyInjector {
  <T> T getInstance(Class<T> clazz);
}

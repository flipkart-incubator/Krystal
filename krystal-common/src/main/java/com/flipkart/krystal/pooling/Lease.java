package com.flipkart.krystal.pooling;

import org.checkerframework.checker.nullness.qual.NonNull;

@SuppressWarnings("ExtendsObject") // For Checker Framework
public interface Lease<T extends Object> extends AutoCloseable {
  @NonNull
  T get();

  @Override
  void close();
}

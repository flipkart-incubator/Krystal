package com.flipkart.krystal.pooling;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface Lease<T extends @NonNull Object> extends AutoCloseable {
  T get();

  @Override
  void close();
}

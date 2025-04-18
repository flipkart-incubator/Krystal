package com.flipkart.krystal.pooling;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface Lease<T extends Object> extends AutoCloseable {
  @NonNull T get();

  @Override
  void close();
}

package com.flipkart.krystal.pooling;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface MultiLeasePool<T extends @NonNull Object> extends AutoCloseable {

  Lease<T> lease() throws LeaseUnavailableException;

  MultiLeasePoolStats stats();

  void close();
}

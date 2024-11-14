package com.flipkart.krystal.utils;

import com.flipkart.krystal.pooling.MultiLeasePoolStats;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface MultiLeasePool<T extends @NonNull Object> extends AutoCloseable {

  Lease<T> lease() throws LeaseUnavailableException;

  MultiLeasePoolStats stats();

  void close();
}

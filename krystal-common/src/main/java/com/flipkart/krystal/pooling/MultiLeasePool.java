package com.flipkart.krystal.pooling;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A pool which can provide multiple leases to the same object.
 *
 * @param <T> The type of object that this pool can lease
 */
public interface MultiLeasePool<T extends @NonNull Object> extends AutoCloseable {

  /**
   * @return a lease to an available object
   * @throws LeaseUnavailableException if for some reason, a lease cannot be provided
   */
  Lease<T> lease() throws LeaseUnavailableException;

  /**
   * @return the current stats of the pool
   */
  MultiLeasePoolStats stats();

  /**
   * Prevents any new leases from being created. Also destroys all objects in the pool when all
   * their leases are closed.
   *
   * <p>This method is idempotent.
   *
   * @see Lease#close()
   */
  @Override
  void close();
}

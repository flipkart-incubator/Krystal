package com.flipkart.krystal.lattice.core.execution;

public enum ThreadingStrategy {
  /**
   * A Request is executed in a single thread. Once the request is completed, the thread may be
   * reused for other requests
   */
  POOLED_NATIVE_THREAD_PER_REQUEST,
  /**
   * One thread is created per core (may or may not be pinned to the core). This means the same
   * thread might concurrently execute multiple requests.
   */
  NATIVE_THREAD_PER_CORE
}

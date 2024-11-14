package com.flipkart.krystal.executors;

import com.flipkart.krystal.pooling.RandomMultiLeasePool;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A pool which is designed to be used in runtimes which intend to dedicate exactly one thread per
 * request. This pool makes sure that a given thread's lease is never held by more than one caller.
 * This guarantee allows clients to implement the thread-per-request execution model thus allowing
 * applications to use features which rely on the current thread's threadlocal - for example:
 * logging MDC, Servlet requestScope Dependency Injection etc.
 */
public class ThreadPerRequestPool extends RandomMultiLeasePool<@NonNull SingleThreadExecutor> {

  public ThreadPerRequestPool(String name, int maxThreads) {
    super(() -> new SingleThreadExecutor(name), 1, maxThreads);
  }
}

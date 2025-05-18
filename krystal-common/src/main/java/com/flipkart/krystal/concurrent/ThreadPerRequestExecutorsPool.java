package com.flipkart.krystal.concurrent;

import com.flipkart.krystal.pooling.RandomMultiLeasePool;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A pool of {@link SingleThreadExecutor}s.
 *
 * <p>This pool which is designed to be used in runtimes which intend to dedicate exactly one thread
 * per request. This pool makes sure that a given thread's lease is never held by more than one
 * caller. This guarantee allows clients to implement the thread-per-request execution model thus
 * allowing applications to use features which rely on the current thread's threadlocal - for
 * example: logging MDC, Servlet requestScope Dependency Injection etc.
 */
public class ThreadPerRequestExecutorsPool
    extends RandomMultiLeasePool<@NonNull SingleThreadExecutor> {

  /**
   * @param name The name of this pool - this is added to the thread names
   * @param maxThreads No more than this number of {@link SingleThreadExecutor}s are created
   */
  public ThreadPerRequestExecutorsPool(String name, int maxThreads) {
    super(
        name, () -> new SingleThreadExecutor(name), 1, maxThreads, SingleThreadExecutor::shutdown);
  }
}

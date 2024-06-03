package com.flipkart.krystal.executors;

import com.flipkart.krystal.utils.MultiLeasePool;
import com.flipkart.krystal.utils.PreferObjectReuse;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * A Lease pool which is designed to be used in runtimes which intend to dedicate one thread per
 * request. This pool makes sure that a given thread's lease is never held by more than one caller.
 * This guarantee allows clients to implement the thread-per-request execution model thus allowing
 * applications to use features which rely on the current thread's threadlocal - for example:
 * logging MDC, Servlet requestScope Dependency Injection etc.
 */
public class ThreadPerRequestPool extends MultiLeasePool<SingleTheadedForkJoinPool> {

  public ThreadPerRequestPool(String name) {
    super(
        () -> new SingleTheadedForkJoinPool(name),
        new PreferObjectReuse(
            /* maxActiveLeasesPerObject= */ 1, /* maxActiveObjects= */ Optional.empty()),
        ExecutorService::shutdown);
  }
}

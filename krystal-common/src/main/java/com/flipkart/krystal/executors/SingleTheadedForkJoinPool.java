package com.flipkart.krystal.executors;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;

public class SingleTheadedForkJoinPool extends ForkJoinPool {

  public SingleTheadedForkJoinPool(String poolName) {
    // Default values picked from Executors.newWorkStealingPool(parallelism)
    super(
        /* parallelism= */ 1, // Set to 1 because maximumPoolSize is 1 anyway.
        /* factory= */ new SingleThreadFactory(poolName),
        /* handler= */ null, /* Same as default */
        /* asyncMode= */ true, /* Same as default - used for async event processing.*/
        /* corePoolSize= */ 1, /* Default is 0. Set to one because we know we will needexactly one thread.*/
        /*
        Make sure not more than 1 thread is active, so that non-thread safe
        code can execute safely.
        */
        /* maximumPoolSize= */ 1,
        /* minimumRunnable= */ 1, /* Same as default */
        /* saturate= */ null, /* Same as default */
        /* keepAliveTime= */ 1, /* Same as default */
        /* unit= */ TimeUnit.MINUTES /* Same as default */);
  }

  private static final class SingleThreadFactory implements ForkJoinWorkerThreadFactory {

    private final String poolName;

    private SingleThreadFactory(String poolName) {
      this.poolName = poolName;
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
      ForkJoinWorkerThread thread = defaultForkJoinWorkerThreadFactory.newThread(pool);
      thread.setName(poolName + '-' + thread.getName());
      return thread;
    }
  }
}

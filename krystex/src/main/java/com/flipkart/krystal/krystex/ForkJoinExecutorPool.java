package com.flipkart.krystal.krystex;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory;

import com.flipkart.krystal.utils.DistributeLeases;
import com.flipkart.krystal.utils.MultiLeasePool;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;

public final class ForkJoinExecutorPool extends MultiLeasePool<ExecutorService> {

  public ForkJoinExecutorPool(double maxParallelismPerCore) {
    //noinspection NumericCastThatLosesPrecision
    super(
        () ->
            // Default values picked from Executors.newWorkStealingPool(parallelism)
            new ForkJoinPool(
                1, // Set to 1 because maximumPoolSize is 1 anyway.
                pool -> {
                  ForkJoinWorkerThread thread = defaultForkJoinWorkerThreadFactory.newThread(pool);
                  thread.setName("Krystal-" + thread.getName());
                  return thread;
                }, // Same as default
                null, // Same as default
                true, // Same as default - used for async event processing.
                1, // Default is 0. We set to one because we know we will need one thread.
                1 /*
                  maximumPoolSize - Make sure not more than 1 thread is active,
                  as KryonExecutor is not thread safe */,
                1, // Same as default
                null, // Same as default
                1, // Same as default
                TimeUnit.MINUTES // Same as default
                ),
        new DistributeLeases(
            max(1, (int) (getRuntime().availableProcessors() * maxParallelismPerCore)), 1),
        ExecutorService::shutdown);
  }
}

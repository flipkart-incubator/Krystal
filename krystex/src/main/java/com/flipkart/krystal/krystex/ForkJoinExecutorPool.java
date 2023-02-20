package com.flipkart.krystal.krystex;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newWorkStealingPool;

import com.flipkart.krystal.utils.DistributeLeases;
import com.flipkart.krystal.utils.MultiLeasePool;
import java.util.concurrent.ExecutorService;

public final class ForkJoinExecutorPool extends MultiLeasePool<ExecutorService>
    implements AutoCloseable {

  public ForkJoinExecutorPool(double maxParallelismPerCore) {
    super(
        () -> newWorkStealingPool(1),
        new DistributeLeases(
            max(1, (int) (getRuntime().availableProcessors() * maxParallelismPerCore)), 1),
        ExecutorService::shutdown);
  }
}

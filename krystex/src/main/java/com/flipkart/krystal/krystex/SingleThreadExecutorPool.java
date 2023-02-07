package com.flipkart.krystal.krystex;

import com.flipkart.krystal.utils.MultiLeasePool;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SingleThreadExecutorPool extends MultiLeasePool<ExecutorService> {
  public static int EXECUTOR_SERVICE_COUNTER = 0;

  public SingleThreadExecutorPool(int maxActiveLeasesPerObject) {
    super(
        () ->
            Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                    .setNameFormat("KrystalNodeExecutor-%s".formatted(EXECUTOR_SERVICE_COUNTER++))
                    .build()),
        maxActiveLeasesPerObject);
  }
}

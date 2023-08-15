package com.flipkart.krystal.krystex;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.flipkart.krystal.utils.MultiLeasePool;
import com.flipkart.krystal.utils.PreferObjectReuse;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public final class SingleThreadExecutorPool extends MultiLeasePool<ExecutorService> {
  private static int executorServiceCounter = 0;

  public SingleThreadExecutorPool(int maxActiveLeasesPerObject) {
    super(
        () ->
            newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                    .setNameFormat("KryonExecutor-%s".formatted(executorServiceCounter++))
                    .build()),
        new PreferObjectReuse(maxActiveLeasesPerObject, Optional.empty()),
        ExecutorService::shutdown);
  }
}

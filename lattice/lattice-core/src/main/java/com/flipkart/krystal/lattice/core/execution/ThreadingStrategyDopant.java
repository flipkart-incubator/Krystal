package com.flipkart.krystal.lattice.core.execution;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant.DOPANT_TYPE;

import com.flipkart.krystal.concurrent.ThreadPerRequestExecutorsPool;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder.BindingKey;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.DopantWithConfig;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategySpec.ThreadingStrategySpecBuilder;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import jakarta.inject.Inject;
import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DopantType(DOPANT_TYPE)
public final class ThreadingStrategyDopant implements DopantWithConfig<ThreadStrategyConfig> {
  static final String DOPANT_TYPE = "krystal.lattice.threadingStrategy";
  private final DependencyInjectionBinder binder;
  private final ThreadingStrategy threadingStrategy;

  private final ThreadPerRequestExecutorsPool executorPool;

  @Inject
  ThreadingStrategyDopant(
      ThreadingStrategySpec spec, ThreadStrategyConfig config, DependencyInjectionBinder binder) {
    this.threadingStrategy = spec.threadingStrategy();
    this.binder = binder;
    this.executorPool =
        switch (threadingStrategy) {
          case NATIVE_THREAD_PER_REQUEST -> new ThreadPerRequestExecutorsPool(
              "ThreadingStrategyDopant-ThreadPerRequestExecutorsPool",
              config.maxApplicationThreads());
          default -> throw new UnsupportedOperationException(threadingStrategy.toString());
        };
  }

  public Lease<? extends ExecutorService> getExecutorService() throws LeaseUnavailableException {
    return executorPool.lease();
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static ThreadingStrategySpecBuilder threadingStrategy() {
    return ThreadingStrategySpec.builder();
  }

  public Closeable openRequestScope(Map<BindingKey, Object> seedMap) {
    return binder.openRequestScope(seedMap, threadingStrategy);
  }
}

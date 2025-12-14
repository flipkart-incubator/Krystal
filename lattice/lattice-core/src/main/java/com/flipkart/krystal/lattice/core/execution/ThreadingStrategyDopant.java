package com.flipkart.krystal.lattice.core.execution;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant.DOPANT_TYPE;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionProvider;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.DopantWithConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@DopantType(DOPANT_TYPE)
public final class ThreadingStrategyDopant implements DopantWithConfig<ThreadingStrategyConfig> {
  public static final String DOPANT_TYPE = "krystal.lattice.threadingStrategy";
  private final DependencyInjectionProvider binder;
  private final ThreadingStrategy threadingStrategy;

  private final SingleThreadExecutorsPool executorPool;
  @Getter private final Function<ExecutorService, ExecutorService> executorServiceTransformer;

  @Inject
  ThreadingStrategyDopant(
      ThreadingStrategySpec spec,
      ThreadingStrategyConfig config,
      DependencyInjectionProvider binder) {
    requireNonNull(
        config,
        "Configuration is mandatory for dopant '"
            + ThreadingStrategyDopant.class.getSimpleName()
            + "' of dopant type '"
            + DOPANT_TYPE
            + "'");
    this.threadingStrategy = spec.threadingStrategy();
    this.binder = binder;
    this.executorServiceTransformer =
        // Reduce all transformer functions to a single one which applies all of them in order
        spec.executorServiceTransformers().stream().reduce(Function.identity(), Function::andThen);
    this.executorPool =
        switch (threadingStrategy) {
          case POOLED_NATIVE_THREAD_PER_REQUEST ->
              new SingleThreadExecutorsPool(
                  "ThreadingStrategyDopant-ThreadPerRequestExecutorsPool",
                  config.maxApplicationThreads());
          default -> throw new UnsupportedOperationException(threadingStrategy.toString());
        };
  }

  public Lease<? extends ExecutorService> getExecutorService() throws LeaseUnavailableException {
    return executorPool.lease();
  }

  public RequestScope openRequestScope(Bindings seedMap) {
    return new RequestScope(binder.openRequestScope(seedMap, threadingStrategy));
  }

  public static class RequestScope implements AutoCloseable {

    private final Closeable delegate;
    private boolean closed;

    public RequestScope(Closeable delegate) {
      this.delegate = delegate;
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      try {
        delegate.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      this.closed = true;
    }
  }
}

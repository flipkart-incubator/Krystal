package com.flipkart.krystal.concurrent;

import static java.lang.Thread.State.TERMINATED;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.DAYS;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A {@link ForkJoinPool} which is guaranteed to have one and exactly one thread. This is useful
 * when there is a need for an eventloop where the event loop thread never blocks.
 *
 * <p>Krystal's default synchronous executor {@code KryonExecutor} relies on this class to pass
 * messages for kryon orchestration.
 */
@Slf4j
public class SingleThreadExecutor extends ForkJoinPool {

  public SingleThreadExecutor(String poolName) {
    // Default values picked from Executors.newWorkStealingPool(parallelism)
    super(
        /* parallelism= */ 1, /* Set to 1 because maximumPoolSize is 1 anyway.*/

        /* factory= */ new SingleThreadFactory(poolName),

        /* handler= */ null, /* Same as default */

        /* asyncMode= */ true, /* Same as default - used for async event processing.*/

        /* corePoolSize= */ 1, /* Default is 0. Set to one because we know we will needexactly one thread.*/

        // Make sure not more than 1 thread is active, so that non-thread safe
        // code can execute safely.
        /* maximumPoolSize= */ 1, /* Default is 32767. Set to 1 to make sure not more than 1 thread is created.*/

        /* minimumRunnable= */ 1, /* Same as default */

        /* saturate= */ null, /* Same as default */

        // Default is 1. Set to high number so that threads are not reclaimed.
        /* keepAliveTime= */ Integer.MAX_VALUE,

        /* unit= */ DAYS /* Default is MINUTES. Set to DAYS so that keepAliveTime is high */);

    // This is to make sure that the execution thread is created before the constructor returns.
    @SuppressWarnings({"initialization.fields.uninitialized", "method.invocation"})
    ForkJoinTask<?> task = this.submit(() -> {});
    task.join();
  }

  @VisibleForTesting
  Thread executionThread() {
    ForkJoinWorkerThread singleThread = ((SingleThreadFactory) getFactory()).singleThread;
    if (singleThread != null) {
      return singleThread;
    } else {
      throw new AssertionError(
          """
          ThreadPerRequestExecutor thread not created yet.\
           This should not happen since we executed a task and waited for its completion in the constructor""");
    }
  }

  public boolean isCurrentThreadTheSingleThread() {
    return currentThread() instanceof ForkJoinWorkerThread forkJoinWorkerThread
        && forkJoinWorkerThread.getPool() == this;
  }

  @Override
  public void execute(Runnable runnable) {
    if (currentThread() == executionThread()) {
      runnable.run();
    } else {
      super.execute(runnable);
    }
  }

  @ToString
  private static final class SingleThreadFactory implements ForkJoinWorkerThreadFactory {
    private final String poolName;
    private @MonotonicNonNull ForkJoinWorkerThread singleThread;

    private volatile boolean threadReturned = false;

    private SingleThreadFactory(String poolName) {
      this.poolName = poolName;
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
      if (threadReturned) {
        log.error(
            """
            ThreadPerRequestExecutor can only have one thread.\
             But `ForkJoinWorkerThreadFactory.newThread` was called more than once.\
             Returning the same thread again.\
             This means the original thread is somehow being stopped,\
             or the ForkjoinPool is creating more than one thread.\
             If this cannot be fixed, then we cannot assume that the initial thread will always be used.""");
      }
      threadReturned = true;
      return singleThread(pool);
    }

    private synchronized ForkJoinWorkerThread singleThread(ForkJoinPool pool) {
      boolean isTerminated = false;
      if (singleThread == null || (isTerminated = TERMINATED.equals(singleThread.getState()))) {
        if (isTerminated) {
          log.error(
              """
              ThreadPerRequestExecutor thread was terminated. \
               This should not happen. Needs investigation. \
               Creating a new thread.""");
        }
        @NonNull ForkJoinWorkerThread thread = defaultForkJoinWorkerThreadFactory.newThread(pool);
        thread.setName(poolName() + '-' + thread.getName());
        this.singleThread = thread;
      }
      return singleThread;
    }

    public String poolName() {
      return poolName;
    }
  }
}

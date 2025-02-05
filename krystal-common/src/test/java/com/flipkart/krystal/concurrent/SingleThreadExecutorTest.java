package com.flipkart.krystal.concurrent;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import org.junit.jupiter.api.Test;

class SingleThreadExecutorTest {

  @Test
  void callingNewThreadOnFactory_alwaysReturnsSameThread() {
    SingleThreadExecutor executor = new SingleThreadExecutor("poolName");
    ForkJoinWorkerThreadFactory factory = executor.getFactory();
    Thread originalThread = factory.newThread(null);
    for (int i = 0; i < 10; i++) {
      Thread newThread = factory.newThread(executor);
      assertThat(originalThread).isEqualTo(newThread);
    }
  }

  @Test
  void stoppingTheOriginalThread_createsANewThread() {
    SingleThreadExecutor executor = new SingleThreadExecutor("poolName");
    ForkJoinWorkerThreadFactory factory = executor.getFactory();
    Thread originalThread = factory.newThread(executor);
    stopThread(originalThread);
    while (originalThread.isAlive()) {
      sleepUninterruptibly(Duration.ofMillis(10));
    }
    Thread newThread = factory.newThread(executor);
    assertThat(originalThread).isNotEqualTo(newThread);
  }

  @SuppressWarnings("deprecation")
  private static void stopThread(Thread originalThread) {
    originalThread.stop();
  }

  @Test
  @SuppressWarnings("FutureReturnValueIgnored")
  void maxPoolSize_shouldBeOne() {
    SingleThreadExecutor executor = new SingleThreadExecutor("poolName");
    for (int i = 0; i < 10; i++) {
      executor.submit(() -> {});
      assertThat(executor.getPoolSize()).isEqualTo(1);
    }
  }

  @Test
  void singleThreadIsRetrievedInConstructor() {
    SingleThreadExecutor executor = new SingleThreadExecutor("poolName");
    assertThat(executor.executionThread()).isNotNull();
  }
}

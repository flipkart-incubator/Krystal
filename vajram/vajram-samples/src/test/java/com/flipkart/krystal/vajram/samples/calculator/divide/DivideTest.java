package com.flipkart.krystal.vajram.samples.calculator.divide;

import static com.flipkart.krystal.config.PropertyNames.RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME;
import static java.lang.Boolean.FALSE;
import static java.lang.Thread.currentThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import org.assertj.core.util.Throwables;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DivideTest {

  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 4);
  }

  private final VajramKryonGraph graph =
      VajramKryonGraph.builder().loadFromPackage(Divide.class.getPackageName()).build();
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void divide_noExternalExecutionPermission_fails() {
    @Nullable String previousValue =
        System.setProperty(
            RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME, FALSE.toString());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .executorId("subtract")
                        .executorService(executorLease.get()))
                .build())) {
      assertThatThrownBy(
              () ->
                  krystexVajramExecutor.execute(
                      Divide_ReqImmutPojo._builder().numerator(5).denominator(7)._build()))
          .isInstanceOf(RejectedExecutionException.class)
          .hasMessage("External invocation is not enabled for vajramId: v<Divide>");
    }
    if (previousValue != null) {
      System.setProperty(RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME, previousValue);
    }
  }

  @Test
  void nonBatchIOVajram_futureCompletesInEventLoopThread() {
    SingleThreadExecutor executorService = executorLease.get();
    CompletableFuture<Thread> eventLoopThreadFuture = new CompletableFuture<>();
    executorService.execute(() -> eventLoopThreadFuture.complete(currentThread()));
    Thread eventLoopThread = eventLoopThreadFuture.join();
    CompletableFuture<@Nullable Integer> result;
    CompletableFuture<Thread> resultThreadFuture = new CompletableFuture<>();
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .executorId("subtract")
                        .executorService(executorService))
                .build())) {
      result =
          krystexVajramExecutor.execute(
              Divide_ReqImmutPojo._builder().numerator(5).denominator(7)._build());
      result.thenRun(
          () -> {
            System.out.println(Throwables.getStackTrace(new RuntimeException()));
            resultThreadFuture.complete(currentThread());
          });
    }
    assertThat(resultThreadFuture.join()).isEqualTo(eventLoopThread);
  }
}

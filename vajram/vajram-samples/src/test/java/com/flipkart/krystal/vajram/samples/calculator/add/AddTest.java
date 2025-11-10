package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static java.lang.Thread.currentThread;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.concurrent.CompletableFuture;
import org.assertj.core.util.Throwables;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddTest {
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 4);
  }

  private final VajramKryonGraph graph = VajramKryonGraph.builder().loadClasses(Add.class).build();
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
  void withOutNumberTwo_usesPlatformDefaultValue_success() {
    // Create a VajramKryonGraph and KrystexVajramExecutor
    VajramKryonGraph graph = VajramKryonGraph.builder().loadClasses(Add.class).build();
    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId("adderTest")
                    .executorService(new SingleThreadExecutor("adderTest")))
            .build();
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor executor = graph.createExecutor(config)) {
      // Execute the Adder Vajram without passing numberTwo
      future =
          executor.execute(
              Add_ReqImmutPojo._builder().numberOne(5)._build(),
              KryonExecutionConfig.builder().build());
    }
    // Assert that the result is equal to numberOne (5) + default numberTwo (0)
    assertThat(future).succeedsWithin(TEST_TIMEOUT).isEqualTo(5);
  }

  @Test
  void batchedIOVajram_futureCompletesInEventLoopThread() {
    SingleThreadExecutor executorService = executorLease.get();
    CompletableFuture<Thread> eventLoopThreadFuture = new CompletableFuture<>();
    executorService.execute(() -> eventLoopThreadFuture.complete(currentThread()));
    Thread eventLoopThread = eventLoopThreadFuture.join();
    CompletableFuture<@Nullable Integer> result;
    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId("subtract")
                    .executorService(executorService))
            .build();
    CompletableFuture<Thread> resultThreadFuture = new CompletableFuture<>();
    try (KrystexVajramExecutor krystexVajramExecutor = graph.createExecutor(config)) {
      result =
          krystexVajramExecutor.execute(
              Add_ReqImmutPojo._builder().numberOne(5)._build(),
              KryonExecutionConfig.builder().build());
      result.thenRun(
          () -> {
            System.out.println(Throwables.getStackTrace(new RuntimeException()));
            resultThreadFuture.complete(currentThread());
          });
    }
    assertThat(resultThreadFuture.join()).isEqualTo(eventLoopThread);
  }
}

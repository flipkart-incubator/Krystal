package com.flipkart.krystal.vajram.samples.calculator;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DoubleMinusOneTest {
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", Runtime.getRuntime().availableProcessors());
  }

  private final VajramGraph graph =
      VajramGraph.builder().loadFromPackage(DoubleMinusOne.class.getPackageName()).build();
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
  void doubleMinusOne_success() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .executorId("doubleMinusOne")
                        .executorService(executorLease.get()))
                .build())) {
      future =
          krystexVajramExecutor.execute(
              DoubleMinusOne_ReqImmutPojo._builder().numbers(List.of(1, 2, 3))._build());
    }
    Assertions.assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(11);
  }

  @Test
  void doubleMinusOne_duplicateNumbers_success() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .executorId("doubleMinusOne")
                        .executorService(executorLease.get()))
                .build())) {
      future =
          krystexVajramExecutor.execute(
              DoubleMinusOne_ReqImmutPojo._builder().numbers(List.of(1, 1, 3))._build());
    }
    Assertions.assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(9);
  }

  @Test
  void doubleMinusOne_noNumbers_success() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .executorId("doubleMinusOne")
                        .executorService(executorLease.get()))
                .build())) {
      future =
          krystexVajramExecutor.execute(
              DoubleMinusOne_ReqImmutPojo._builder().numbers(List.of())._build());
    }
    Assertions.assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(-1);
  }
}

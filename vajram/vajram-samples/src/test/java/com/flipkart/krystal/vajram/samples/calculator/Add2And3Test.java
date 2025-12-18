package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import java.util.concurrent.CompletableFuture;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Add2And3Test {
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 4);
  }

  private final VajramGraph graph =
      VajramGraph.builder().loadFromPackage(Add2And3.class.getPackageName()).build();

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
  void add2And3_success() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        KrystexGraph.builder()
            .vajramGraph(graph)
            .build()
            .createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .kryonExecutorConfigBuilder(
                        KryonExecutorConfig.builder()
                            .executorId("add2and3")
                            .executorService(executorLease.get()))
                    .build())) {
      future = krystexVajramExecutor.execute(Add2And3_ReqImmutPojo._builder()._build());
    }
    Assertions.assertThat(future).succeedsWithin(TEST_TIMEOUT).isEqualTo(5);
  }
}

package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
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

  private final VajramKryonGraph graph =
      VajramKryonGraph.builder().loadFromPackage(Add2And3.class.getPackageName()).build();

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
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .requestId("add2and3")
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()))
                .build())) {
      future =
          krystexVajramExecutor.execute(
              graph.getVajramIdByVajramDefType(Add2And3.class), Add2And3_ImmutReqPojo._builder()._build());
    }
    Assertions.assertThat(future).succeedsWithin(TEST_TIMEOUT).isEqualTo(5);
  }
}

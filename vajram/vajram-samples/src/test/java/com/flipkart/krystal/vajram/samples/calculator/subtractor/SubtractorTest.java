package com.flipkart.krystal.vajram.samples.calculator.subtractor;

import static com.flipkart.krystal.vajram.VajramID.ofVajram;

import com.flipkart.krystal.executors.SingleThreadExecutor;
import com.flipkart.krystal.executors.ThreadPerRequestPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.utils.Lease;
import com.flipkart.krystal.utils.LeaseUnavailableException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubtractorTest {

  private static ThreadPerRequestPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new ThreadPerRequestPool("RequestLevelCacheTest", 4);
  }

  private final VajramKryonGraph graph =
      VajramKryonGraph.builder().loadFromPackage(Subtractor.class.getPackageName()).build();
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
  void subtract_success() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .requestId("subtract")
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()))
                .build())) {
      future =
          krystexVajramExecutor.execute(
              ofVajram(Subtractor.class),
              SubtractorRequest.builder().numberOne(5).numberTwo(7).build());
    }
    Assertions.assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(-2);
  }
}

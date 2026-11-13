package com.flipkart.krystal.vajram.samples.batcheddep;

import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.flipkart.krystal.vajram.samples.Util.loadFromClasspath;
import static com.flipkart.krystal.vajram.samples.Util.simpleInputBatcher;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.KrystalExecutorConfig;
import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.batching.InputBatcherStrategy.CustomBatcherStrategy;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.samples.calculator.multiply.Multiply;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies that a {@code @Batched} dependency is read via the batch items of a batched call. */
class BatchedDependencyTest {
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("BatchedDependencyTest", 4);
  }

  private final VajramGraph graph =
      loadFromClasspath(BatchedDependency.class.getPackageName(), Multiply.class.getPackageName())
          .build();

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    BatchedDependency.BATCH_CALLS.reset();
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void batchedDependency_isReadViaBatchItems() {
    VajramID vajramId = graph.getVajramIdByVajramDefType(BatchedDependency.class);
    CompletableFuture<Integer> first;
    CompletableFuture<Integer> second;
    try (VajramKryonExecutor executor =
        KrystexGraph.builder()
            .vajramGraph(graph)
            .externallyInvocableVajramIds(ImmutableSet.of(BatchedDependency_Req._VAJRAM_ID))
            .inputBatcherStrategy(new CustomBatcherStrategy(simpleInputBatcher(graph, vajramId)))
            .build()
            .createExecutor(
                KrystalExecutorConfig.builder()
                    .executorId("batchedDependency")
                    .executorService(executorLease.get()))) {
      first = executor.execute(BatchedDependency_ReqImmutPojo._builder().id(5)._build());
      second = executor.execute(BatchedDependency_ReqImmutPojo._builder().id(7)._build());
    }
    // id * 2 + 1
    assertThat(first).succeedsWithin(TEST_TIMEOUT).isEqualTo(11);
    assertThat(second).succeedsWithin(TEST_TIMEOUT).isEqualTo(15);
    // There are no batch keys, so both requests are in the same batch
    assertThat(BatchedDependency.BATCH_CALLS.sum()).isEqualTo(1);
  }
}

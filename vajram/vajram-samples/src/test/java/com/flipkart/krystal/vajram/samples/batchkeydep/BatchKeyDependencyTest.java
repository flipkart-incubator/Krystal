package com.flipkart.krystal.vajram.samples.batchkeydep;

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

/**
 * Verifies that requests are grouped by the {@code @BatchesGroupedBy} input ({@code base}), and
 * that {@code base} and the dependency derived from it ({@code lookup}) are read individually in
 * the batched output, while {@code @Batched} {@code id} is read via the batch items.
 */
class BatchKeyDependencyTest {
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("BatchKeyDependencyTest", 4);
  }

  private final VajramGraph graph =
      loadFromClasspath(BatchKeyDependency.class.getPackageName(), Multiply.class.getPackageName())
          .build();

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    BatchKeyDependency.BATCH_CALLS.reset();
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void requestsAreGroupedByBatchKey_andReadKeyAndBaseIndividually() {
    VajramID vajramId = graph.getVajramIdByVajramDefType(BatchKeyDependency.class);
    CompletableFuture<Integer> first;
    CompletableFuture<Integer> second;
    CompletableFuture<Integer> third;
    try (VajramKryonExecutor executor =
        KrystexGraph.builder()
            .vajramGraph(graph)
            .externallyInvocableVajramIds(ImmutableSet.of(BatchKeyDependency_Req._VAJRAM_ID))
            .inputBatcherStrategy(new CustomBatcherStrategy(simpleInputBatcher(graph, vajramId)))
            .build()
            .createExecutor(
                KrystalExecutorConfig.builder()
                    .executorId("batchKeyDependency")
                    .executorService(executorLease.get()))) {
      first = executor.execute(BatchKeyDependency_ReqImmutPojo._builder().id(5).base(100)._build());
      second =
          executor.execute(BatchKeyDependency_ReqImmutPojo._builder().id(7).base(100)._build());
      // Different base => different batch key
      third = executor.execute(BatchKeyDependency_ReqImmutPojo._builder().id(9).base(50)._build());
    }
    // lookup = Multiply(base, <absent>) = 100. result = id + base + lookup
    assertThat(first).succeedsWithin(TEST_TIMEOUT).isEqualTo(205);
    assertThat(second).succeedsWithin(TEST_TIMEOUT).isEqualTo(207);
    assertThat(third).succeedsWithin(TEST_TIMEOUT).isEqualTo(109);
    // The first two requests share the batch key (base=100) and hence one batched call. The third
    // has a different batch key (base=50) and gets its own.
    assertThat(BatchKeyDependency.BATCH_CALLS.sum()).isEqualTo(2);
  }
}

package com.flipkart.krystal.vajram.samples.calculator.addzero;

import static com.flipkart.krystal.vajram.samples.Util.loadFromClasspath;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.batching.DepChainBatcherConfig;
import com.flipkart.krystal.vajramexecutor.krystex.batching.InputBatcherConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddZeroTest {
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", Runtime.getRuntime().availableProcessors());
  }

  private VajramKryonGraphBuilder graph;
  private static final String REQUEST_ID = "addZeroTest";

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    this.graph = loadFromClasspath(AddZero.class.getPackageName(), Add.class.getPackageName());
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void addZero_missingResolver_success() {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    VajramID vajramID = graph.getVajramIdByVajramDefType(Add.class);
    graph.registerInputBatchers(
        new InputBatcherConfig(
            ImmutableMap.of(
                vajramID,
                ImmutableList.of(DepChainBatcherConfig.simple(() -> new InputBatcherImpl(100))))));
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .executorId(REQUEST_ID)
                        .executorService(executorLease.get()))
                .build())) {
      future =
          krystexVajramExecutor.execute(
              AddZero_ReqImmutPojo._builder().number(5)._build(),
              KryonExecutionConfig.builder().executionId("addZeroTest").build());
    }
    assertThat(future).succeedsWithin(1, SECONDS).isEqualTo(5);
  }
}

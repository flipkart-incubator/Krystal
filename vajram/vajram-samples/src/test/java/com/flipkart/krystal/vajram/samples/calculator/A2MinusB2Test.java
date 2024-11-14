package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.executors.SingleThreadExecutor;
import com.flipkart.krystal.executors.ThreadPerRequestPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.utils.Lease;
import com.flipkart.krystal.utils.LeaseUnavailableException;
import com.flipkart.krystal.vajram.samples.Util;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.Builder;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class A2MinusB2Test {
  private static final String REQUEST_ID = "A2MinusB2Test";
  private static ThreadPerRequestPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new ThreadPerRequestPool("RequestLevelCacheTest", 4);
  }

  private Builder graph;
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    graph = Util.loadFromClasspath(A2MinusB2.class.getPackageName());
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void success() {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()))
                .requestId(REQUEST_ID)
                .build())) {
      future = executeVajram(krystexVajramExecutor, A2MinusB2Request.builder().a(3).b(2).build());
    }
    //noinspection AssertBetweenInconvertibleTypes https://youtrack.jetbrains.com/issue/IDEA-342354
    assertThat(future).succeedsWithin(1, HOURS).isEqualTo(2);
  }

  private static CompletableFuture<Integer> executeVajram(
      KrystexVajramExecutor krystexVajramExecutor, A2MinusB2Request req) {
    return krystexVajramExecutor.execute(
        vajramID(getVajramIdString(A2MinusB2.class)),
        req,
        KryonExecutionConfig.builder().executionId("1").build());
  }
}

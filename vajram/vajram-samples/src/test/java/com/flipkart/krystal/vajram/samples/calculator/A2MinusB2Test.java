package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.samples.Util;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.Builder;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class A2MinusB2Test {
  private Builder graph;
  private static final String REQUEST_ID = "A2MinusB2Test";

  @BeforeEach
  void setUp() {
    graph = Util.loadFromClasspath(A2MinusB2.class.getPackageName());
  }

  @Test
  void success() {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    try (KrystexVajramExecutor<ApplicationRequestContext> krystexVajramExecutor =
        graph.createExecutor(() -> REQUEST_ID)) {
      future = executeVajram(krystexVajramExecutor, A2MinusB2Request.builder().a(3).b(2).build());
    }
    //noinspection AssertBetweenInconvertibleTypes https://youtrack.jetbrains.com/issue/IDEA-342354
    assertThat(future).succeedsWithin(1, HOURS).isEqualTo(2);
  }

  private static CompletableFuture<Integer> executeVajram(
      KrystexVajramExecutor<ApplicationRequestContext> krystexVajramExecutor,
      A2MinusB2Request req) {
    return krystexVajramExecutor.execute(
        vajramID(getVajramIdString(A2MinusB2.class)),
        rc -> req,
        KryonExecutionConfig.builder().executionId("1").build());
  }
}

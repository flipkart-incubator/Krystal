package com.flipkart.krystal.vajram.samples.calculator.subtractor;

import static com.flipkart.krystal.vajram.VajramID.ofVajram;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SubtractorTest {

  private final VajramKryonGraph graph =
      VajramKryonGraph.builder().loadFromPackage(Subtractor.class.getPackageName()).build();

  @Test
  void subtract_success() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor<ApplicationRequestContext> krystexVajramExecutor =
        graph.createExecutor(() -> "subtract")) {
      future =
          krystexVajramExecutor.execute(
              ofVajram(Subtractor.class),
              SubtractorRequest.builder().numberOne(5).numberTwo(7).build());
    }
    Assertions.assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(-2);
  }
}

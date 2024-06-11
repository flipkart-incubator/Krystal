package com.flipkart.krystal.vajram.samples.calculator.multiplier;

import static com.flipkart.krystal.vajram.VajramID.ofVajram;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class MultiplierTest {

  private final VajramKryonGraph graph =
      VajramKryonGraph.builder().loadFromPackage(Multiplier.class.getPackageName()).build();

  @Test
  void multiply_success() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor<ApplicationRequestContext> krystexVajramExecutor =
        graph.createExecutor(() -> "multiply")) {
      future =
          krystexVajramExecutor.execute(
              ofVajram(Multiplier.class),
              MultiplierRequest.builder().numberOne(3).numberTwo(9).build());
    }
    Assertions.assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(27);
  }
}

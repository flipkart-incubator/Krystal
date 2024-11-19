package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.VajramID.ofVajram;
import static org.junit.jupiter.api.Assertions.*;

import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DoubleMinusOneTest {

  private final VajramKryonGraph graph =
      VajramKryonGraph.builder().loadFromPackage(DoubleMinusOne.class.getPackageName()).build();

  @Test
  void doubleMinusOne_success() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder().requestId("doubleMinusOne").build())) {
      future =
          krystexVajramExecutor.execute(
              ofVajram(DoubleMinusOne.class),
              DoubleMinusOneRequest.builder().numbers(List.of(1, 2, 3)).build());
    }
    Assertions.assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(11);
  }
}

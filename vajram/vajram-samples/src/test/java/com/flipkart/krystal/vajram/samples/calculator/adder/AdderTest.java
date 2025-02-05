package com.flipkart.krystal.vajram.samples.calculator.adder;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class AdderTest {

  @Test
  void withOutNumberTwo_usesPlatformDefaultValue_success() {
    // Create a VajramKryonGraph and KrystexVajramExecutor
    VajramKryonGraph graph = VajramKryonGraph.builder().loadClass(Adder.class).build();
    KrystexVajramExecutorConfig config =
        KrystexVajramExecutorConfig.builder()
            .requestId("adderTest")
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .singleThreadExecutor(new SingleThreadExecutor("adderTest"))
                    ._riskyOpenAllKryonsForExternalInvocation(true))
            .build();
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor executor = graph.createExecutor(config)) {
      // Execute the Adder Vajram without passing numberTwo
      future =
          executor.execute(
              graph.getVajramId(Adder.class),
              Adder_ImmutReqPojo._builder().numberOne(5)._build(),
              KryonExecutionConfig.builder().build());
    }
    // Assert that the result is equal to numberOne (5) + default numberTwo (0)
    assertThat(future).succeedsWithin(1, SECONDS).isEqualTo(5);
  }
}

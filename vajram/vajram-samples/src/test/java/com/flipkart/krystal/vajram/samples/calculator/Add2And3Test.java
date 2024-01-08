package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.vajram.VajramID.ofVajram;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class Add2And3Test {

  private final VajramKryonGraph graph =
      VajramKryonGraph.builder().loadFromPackage(Formula.class.getPackageName()).build();

  @Test
  void testAdd2And3() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor<ApplicationRequestContext> krystexVajramExecutor =
        graph.createExecutor(() -> "add2and3")) {
      future =
          krystexVajramExecutor.execute(
              ofVajram(Add2And3.class),
              applicationRequestContext -> Add2And3Request.builder().build());
    }
    Assertions.assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(5);
  }
}

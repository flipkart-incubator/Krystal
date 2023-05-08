package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.samples.Util.javaMethodBenchmark;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder.add;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.Divider.divide;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.samples.Util;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph.Builder;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FormulaTest {

  private Builder graph;

  @BeforeEach
  void setUp() {
    graph = loadFromClasspath("com.flipkart.krystal.vajram.samples.benchmarks.calculator");
  }

  @Test
  void formula_success() throws ExecutionException, InterruptedException {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
        graph.build().createExecutor(new FormulaRequestContext(100, 20, 5, "formulaTest"))) {
      future = executeVajram(krystexVajramExecutor, 100);
    }
    assertThat(future.get()).isEqualTo(4);
  }

  @Test
  void vajram_benchmark() throws Exception {
    int loopCount = 1_000_000;
    VajramNodeGraph graph = this.graph.maxParallelismPerCore(5).build();
    long javaNativeTime = javaMethodBenchmark(FormulaTest::syncFormula, loopCount);
    long javaFuturesTime = Util.javaFuturesBenchmark(FormulaTest::asyncFormula, loopCount);
    //noinspection unchecked
    Thread.sleep(5000);
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    long startTime = System.nanoTime();
    for (int value = 0; value < loopCount; value++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
          graph.createExecutor(new FormulaRequestContext(100, 20, 5, "formulaTest"))) {
        timeToCreateExecutors += System.nanoTime() - iterStartTime;
        long enqueueStart = System.nanoTime();
        futures[value] = executeVajram(krystexVajramExecutor, value);
        timeToEnqueueVajram += System.nanoTime() - enqueueStart;
      }
    }
    System.out.printf("Avg. time to Create Executors:%,d %n", timeToCreateExecutors / loopCount);
    System.out.printf("Avg. time to Enqueue vajrams:%,d %n", timeToEnqueueVajram / loopCount);
    allOf(futures).join();
    long vajramTime = System.nanoTime() - startTime;
    System.out.printf("Avg. time to execute vajrams:%,d %n", vajramTime / loopCount);
    allOf(futures)
        .whenComplete(
            (unused, throwable) -> {
              for (CompletableFuture<Integer> future : futures) {
                assertThat(future.getNow(0)).isEqualTo(4);
              }
            });
    System.out.printf(
        "Platform overhead over native code: %,.0f ns per request%n",
        (1.0 * vajramTime - javaNativeTime) / loopCount);
    /*
     * Benchmark config:
     *    loopCount = 1_000_000
     *    maxParallelismPerCore = 5
     *    Processor: 2.6 GHz 6-Core Intel Core i7
     * Benchmark result:
     *    platform overhead over reactive code = ~13 µs (13,000 ns) per request
     *    maxPoolSize = 60
     *    maxActiveLeasesPerObject: 234
     *    peakAvgActiveLeasesPerObject: 140.53
     */
    System.out.printf(
        "Platform overhead over reactive code: %,.0f ns per request%n",
        (1.0 * vajramTime - javaFuturesTime) / loopCount);
    System.out.printf(
        "maxActiveLeasesPerObject: %s, peakAvgActiveLeasesPerObject: %s, maxPoolSize: %s%n",
        graph.getExecutorPool().maxActiveLeasesPerObject(),
        graph.getExecutorPool().peakAvgActiveLeasesPerObject(),
        graph.getExecutorPool().maxPoolSize());
  }

  private static CompletableFuture<Integer> executeVajram(
      KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor, int value) {
    return krystexVajramExecutor.execute(
        vajramID(Formula.ID),
        rc -> FormulaRequest.builder().a(value).p(rc.p).q(rc.q).build(),
        "formulaTest" + value);
  }

  private static void syncFormula(Integer value) {
    //noinspection ResultOfMethodCallIgnored
    divide(value, add(20, 5));
  }

  private static CompletableFuture<Integer> asyncFormula(int value) {
    CompletableFuture<Integer> numerator = completedFuture(value);
    CompletableFuture<Integer> add1 = completedFuture(20);
    CompletableFuture<Integer> add2 = completedFuture(5);
    CompletableFuture<Integer> sum =
        allOf(add1, add2).thenApply(unused -> add(add1.getNow(null), add2.getNow(null)));
    return allOf(numerator, sum)
        .thenApply(unused -> divide(numerator.getNow(null), sum.getNow(null)));
  }

  private record FormulaRequestContext(int a, int p, int q, String requestId)
      implements ApplicationRequestContext {}

  private static Builder loadFromClasspath(String... packagePrefixes) {
    Builder builder = VajramNodeGraph.builder();
    Arrays.stream(packagePrefixes).forEach(builder::loadFromPackage);
    return builder;
  }
}

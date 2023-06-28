package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.samples.Util.javaMethodBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.printStats;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder.add;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.Divider.divide;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.krystex.node.KrystalNodeExecutor;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutorMetrics;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.samples.Util;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph.Builder;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
class FormulaTest {

  private Builder graph;

  @BeforeEach
  void setUp() {
    graph = loadFromClasspath("com.flipkart.krystal.vajram.samples.benchmarks.calculator");
  }

  @Test
  @Order(1)
  void formula_success() throws Exception {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
        graph.build().createExecutor(new FormulaRequestContext(100, 20, 5, "formulaTest"))) {
      future = executeVajram(krystexVajramExecutor, 0);
    }
    assertThat(future.get()).isEqualTo(4);
  }

  @Disabled("Long running benchmark (~16s)")
  @Test
  @Order(2)
  void vajram_benchmark() throws Exception {
    int loopCount = 1_000_000;
    VajramNodeGraph graph = this.graph.maxParallelismPerCore(5).build();
    long javaNativeTimeNs = javaMethodBenchmark(FormulaTest::syncFormula, loopCount);
    long javaFuturesTimeNs = Util.javaFuturesBenchmark(FormulaTest::asyncFormula, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KrystalNodeExecutorMetrics[] metrics = new KrystalNodeExecutorMetrics[loopCount];
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    long startTime = System.nanoTime();
    for (int value = 0; value < loopCount; value++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
          graph.createExecutor(new FormulaRequestContext(100, 20, 5, "formulaTest"))) {
        timeToCreateExecutors += System.nanoTime() - iterStartTime;
        metrics[value] =
            ((KrystalNodeExecutor) krystexVajramExecutor.getKrystalExecutor())
                .getKrystalNodeMetrics();
        long enqueueStart = System.nanoTime();
        futures[value] = executeVajram(krystexVajramExecutor, value);
        timeToEnqueueVajram += System.nanoTime() - enqueueStart;
      }
    }
    allOf(futures).join();
    long vajramTimeNs = System.nanoTime() - startTime;
    allOf(futures)
        .whenComplete(
            (unused, throwable) -> {
              for (CompletableFuture<Integer> future : futures) {
                assertThat(future.getNow(0)).isEqualTo(4);
              }
            });
    /*
     * Benchmark config:
     *    loopCount = 1_000_000
     *    maxParallelismPerCore = 10
     *    Processor: 2.6 GHz 6-Core Intel Core i7 (with hyperthreading - 12 virtual cores)
     * Benchmark result:
     *    platform overhead over reactive code = ~13 µs (13,000 ns) per request
     *    maxPoolSize = 120
     *    maxActiveLeasesPerObject: 170
     *    peakAvgActiveLeasesPerObject: 122
     *    Avg. time to Enqueue vajrams : 8,486 ns
     *    Avg. time to execute vajrams : 14,965 ns
     *    Throughput executions/sec: 71000
     */
    printStats(
        loopCount,
        graph,
        javaNativeTimeNs,
        javaFuturesTimeNs,
        metrics,
        timeToCreateExecutors,
        timeToEnqueueVajram,
        vajramTimeNs);
  }

  @Disabled("Long running benchmark (~16s)")
  @Test
  @Order(3)
  void vajram_benchmark_2() throws Exception {
    int outerLoopCount = 1_000;
    int innerLoopCount = 1000;
    int loopCount = outerLoopCount * innerLoopCount;
    VajramNodeGraph graph = this.graph.maxParallelismPerCore(1).build();
    long javaNativeTimeNs = javaMethodBenchmark(FormulaTest::syncFormula, loopCount);
    long javaFuturesTimeNs = Util.javaFuturesBenchmark(FormulaTest::asyncFormula, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KrystalNodeExecutorMetrics[] metrics = new KrystalNodeExecutorMetrics[outerLoopCount];
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    long startTime = System.nanoTime();
    for (int outer_i = 0; outer_i < outerLoopCount; outer_i++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
          graph.createExecutor(new FormulaRequestContext(100, 20, 5, "formulaTest"))) {
        timeToCreateExecutors += System.nanoTime() - iterStartTime;
        metrics[outer_i] =
            ((KrystalNodeExecutor) krystexVajramExecutor.getKrystalExecutor())
                .getKrystalNodeMetrics();
        for (int inner_i = 0; inner_i < innerLoopCount; inner_i++) {
          int iterationNum = outer_i * innerLoopCount + inner_i;
          long enqueueStart = System.nanoTime();
          futures[iterationNum] = executeVajram(krystexVajramExecutor, iterationNum);
          timeToEnqueueVajram += System.nanoTime() - enqueueStart;
        }
      }
    }
    allOf(futures).join();
    long vajramTimeNs = System.nanoTime() - startTime;
    allOf(futures)
        .whenComplete(
            (unused, throwable) -> {
              for (CompletableFuture<Integer> future : futures) {
                assertThat(future.getNow(0)).isEqualTo(4);
              }
            });
    printStats(
        outerLoopCount,
        innerLoopCount,
        graph,
        javaNativeTimeNs,
        javaFuturesTimeNs,
        metrics,
        timeToCreateExecutors,
        timeToEnqueueVajram,
        vajramTimeNs);
  }

  private static CompletableFuture<Integer> executeVajram(
      KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor, int value) {
    return krystexVajramExecutor.execute(
        vajramID(Formula.ID),
        rc -> FormulaRequest.builder().a(rc.a).p(rc.p + value).q(rc.q + value).build(),
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

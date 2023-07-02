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
import com.flipkart.krystal.krystex.node.NodeExecutionConfig;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.modulation.Batcher;
import com.flipkart.krystal.vajram.samples.Util;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder;
import com.flipkart.krystal.vajramexecutor.krystex.InputModulatorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph.Builder;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
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
    Adder.CALL_COUNTER.reset();
  }

  @Test
  @Order(1)
  void formula_success() throws Exception {
    CompletableFuture<Integer> future;
    VajramNodeGraph graph = this.graph.build();
    graph.registerInputModulators(
        vajramID(Adder.ID), InputModulatorConfig.simple(() -> new Batcher<>(100)));
    try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
        graph.createExecutor(new FormulaRequestContext(100, 20, 5, "formulaTest"))) {
      future = executeVajram(krystexVajramExecutor, 0);
    }
    assertThat(future.get()).isEqualTo(4);
    assertThat(Adder.CALL_COUNTER.sum()).isEqualTo(1);
  }

  //  @Disabled("Long running benchmark (~16s)")
  @Test
  @Order(2)
  void vajram_benchmark() throws Exception {
    int loopCount = 1_000_000;
    VajramNodeGraph graph = this.graph.maxParallelismPerCore(5).build();
    graph.registerInputModulators(
        vajramID(Adder.ID), InputModulatorConfig.simple(() -> new Batcher<>(100)));
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
    assertThat(
            allOf(futures)
                .whenComplete(
                    (unused, throwable) -> {
                      for (int i = 0, futuresLength = futures.length; i < futuresLength; i++) {
                        CompletableFuture<Integer> future = futures[i];
                        assertThat(future.getNow(0)).isEqualTo((100 + i) / (20 + i + 5 + i));
                      }
                    }))
        .succeedsWithin(Duration.ofSeconds(1));
    assertThat(Adder.CALL_COUNTER.sum()).isEqualTo(loopCount);

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

  //  @Disabled("Long running benchmark (~16s)")
  @Test
  @Order(3)
  void vajram_benchmark_2() throws Exception {
    int outerLoopCount = 1000;
    int innerLoopCount = 1_000;
    /*
      krystalNodeExecutor.nodeExecStrategy = BATCH;
      int outerLoopCount = 1;
      int innerLoopCount = 1_000;
        computeNodeCommandsTime         242,307,701 ns,
        propagateNodeCommands           17,255,374 ns,
        executeMainLogicTime            140,084,856 ns
    */
    /*
      krystalNodeExecutor.nodeExecStrategy = BATCH;
      int outerLoopCount = 1;
      int innerLoopCount = 2_000;
        computeNodeCommandsTime         309,175,222 ns,
        propagateNodeCommands           22,042,204 ns,
        executeMainLogicTime            260,570,055 ns
    */
    /*
      int innerLoopCount = 3_000;
      int outerLoopCount = 1;
        krystalNodeExecutor.nodeExecStrategy = BATCH;
          computeNodeCommandsTime         481,568,909 ns,
          propagateNodeCommands           37,506,122 ns,
          executeMainLogicTime            370,869,072 ns
        krystalNodeExecutor.nodeExecStrategy = GRANULAR;
          computeNodeCommandsTime=        364,815,805 ns,
          propagateNodeCommands=        1,104,913,190 ns,
          executeMainLogicTime=           334,096,017 ns
    */
    /*
      int outerLoopCount = 1;
      int innerLoopCount = 4_000;
        krystalNodeExecutor.nodeExecStrategy = BATCH;
          computeNodeCommandsTime         482,436,145 ns,
          propagateNodeCommands           36,976,695 ns,
          executeMainLogicTime            340,330,908 ns
        krystalNodeExecutor.nodeExecStrategy = GRANULAR;
          computeNodeCommandsTime=        434,464,327 ns,
          propagateNodeCommands=        1,901,071,789 ns,
          executeMainLogicTime=           364,970,125 ns
    */
    /*
      int outerLoopCount = 1;
      int innerLoopCount = 10_000;
        krystalNodeExecutor.nodeExecStrategy = BATCH;
          computeNodeCommandsTime         762,694,150 ns,
          propagateNodeCommands           75,638,292 ns,
          executeMainLogicTime            689,111,577 ns
        krystalNodeExecutor.nodeExecStrategy = GRANULAR;
          computeNodeCommandsTime=        845,271,269 ns,
          propagateNodeCommands=       12,688,314,941 ns,
          executeMainLogicTime=           748,000,652 ns
    */

    int loopCount = outerLoopCount * innerLoopCount;
    VajramNodeGraph graph = this.graph.maxParallelismPerCore(1).build();
    graph.registerInputModulators(
        vajramID(Adder.ID), InputModulatorConfig.simple(() -> new Batcher<>(innerLoopCount)));
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
        KrystalNodeExecutorMetrics krystalNodeMetrics =
            ((KrystalNodeExecutor) krystexVajramExecutor.getKrystalExecutor())
                .getKrystalNodeMetrics();
        metrics[outer_i] = krystalNodeMetrics;
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
    assertThat(
            allOf(futures)
                .whenComplete(
                    (unused, throwable) -> {
                      for (int i = 0, futuresLength = futures.length; i < futuresLength; i++) {
                        CompletableFuture<Integer> future = futures[i];
                        assertThat(future.getNow(0)).isEqualTo((100 + i) / (20 + i + 5 + i));
                      }
                    }))
        .succeedsWithin(Duration.ofSeconds(1));
    assertThat(Adder.CALL_COUNTER.sum()).isEqualTo(outerLoopCount);
    /*
       Old code performance:
       Total java method time: 29,883,631
       Total java futures time: 66,435,359
       Loop Count: 1,000,000
       Avg. time to Create Executors:14,522 ns
       Avg. time to Enqueue vajrams:2,904 ns
       Avg. time to execute vajrams:190,280 ns
       Throughput executions/s: 5263
       CommandsQueuedCount: 1,003,999
       CommandQueueBypassedCount: 9,998,637
       Platform overhead over native code: 190,251 ns per request
       Platform overhead over reactive code: 190,214 ns per request
       maxActiveLeasesPerObject: 165, peakAvgActiveLeasesPerObject: 164.66666666666666, maxPoolSize: 12
    */
    /*
      Total java method time: 6,624,273
      Total java futures time: 65,579,807
      Loop Count: 1,000,000
      Avg. time to Create Executors:16,842 ns
      Avg. time to Enqueue vajrams:4,125 ns
      Avg. time to execute vajrams:24,266 ns
      Throughput executions/s: 41666
      CommandsQueuedCount: 1,002,000
      CommandQueueBypassedCount: 6,003,000
      Platform overhead over native code: 24,260 ns per request
      Platform overhead over reactive code: 24,201 ns per request
      maxActiveLeasesPerObject: 72, peakAvgActiveLeasesPerObject: 71.33333333333333, maxPoolSize: 12
    */
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
        rc -> FormulaRequest.builder().a(rc.a + value).p(rc.p + value).q(rc.q + value).build(),
        NodeExecutionConfig.builder().executionId("formulaTest" + value).build());
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

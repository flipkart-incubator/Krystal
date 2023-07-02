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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FormulaTest {

  private Builder graph;

  @BeforeEach
  void setUp() {
    graph = loadFromClasspath("com.flipkart.krystal.vajram.samples.benchmarks.calculator");
    Adder.CALL_COUNTER.reset();
  }

  @Test
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

  @Disabled("Long running benchmark (~16s)")
  @Test
  void vajram_benchmark() throws Exception {
    int loopCount = 1_000_000;
    VajramNodeGraph graph = this.graph.maxParallelismPerCore(10).build();
    graph.registerInputModulators(
        vajramID(Adder.ID), InputModulatorConfig.simple(() -> new Batcher<>(100)));
    long javaNativeTimeNs = javaMethodBenchmark(FormulaTest::syncFormula, loopCount);
    long javaFuturesTimeNs = Util.javaFuturesBenchmark(FormulaTest::asyncFormula, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KrystalNodeExecutorMetrics[] metrics = new KrystalNodeExecutorMetrics[loopCount];
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    /*
    GRANULAR + INCREMETAL
    Throughput executions/s: 33971
    KrystalNodeExecutorMetrics{
      totalNodeTimeNs                 4m 15s 259366054ns
      computeNodeCommandsTime         2m 9s 246590027ns
      propagateNodeCommands           0m 12s 820437733ns
      executeMainLogicTime            1m 4s 789491305ns
      registerDepCallbacksTime        0m 8s 84212158ns
      allResolversExecutedTime        0m 0s 0ns
      flushDependencyIfNeededTime     0m 8s 90879031ns

      commandQueuedCount              12,000,000
      commandQueueBypassedCount       0
      nodeInputsBatchCount            0
      depCallbackBatchCount           0
      executeMainLogicCount           3,000,000


      BATCH + ONE_SHOT
      Throughput executions/s: 31688
      KrystalNodeExecutorMetrics{
        totalNodeTimeNs                 4m 56s 161824764ns
        computeNodeCommandsTime         2m 8s 977622263ns
        propagateNodeCommands           0m 38s 102987106ns
        executeMainLogicTime            1m 9s 610911434ns
        registerDepCallbacksTime        0m 27s 213733999ns
        allResolversExecutedTime        0m 7s 771743138ns
        flushDependencyIfNeededTime     0m 8s 681010976ns

        commandQueuedCount              11,000,000
        commandQueueBypassedCount       0
        nodeInputsBatchCount            3,000,000
        depCallbackBatchCount           2,000,000
        executeMainLogicCount           3,000,000
      }
    }
     */
    long startTime = System.nanoTime();
    for (int value = 0; value < loopCount; value++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
          graph.createExecutor(
              new FormulaRequestContext(100, 20, 5, "formulaTest")
              //
              /*   ,
              KrystalNodeExecutorConfig.builder()
                  .nodeExecStrategy(NodeExecStrategy.GRANULAR)
                  .dependencyExecStrategy(DependencyExecStrategy.INCREMENTAL)
                  .build()*/
              //
              )) {
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
/*
BATCH
int outerLoopCount = 100;
int innerLoopCount = 10000;
Throughput executions/s: 24379
KrystalNodeExecutorMetrics{
  totalNodeTimeNs                 7m 19s 46633901ns
  computeNodeCommandsTime         3m 22s 103212344ns
  propagateNodeCommands           0m 17s 169845765ns
  executeMainLogicTime            2m 28s 354621641ns
  registerDepCallbacksTime        0m 10s 958042969ns
  handleResolverCommandTime       0m 22s 262713588ns
  executeResolversTime            1m 16s 966988741ns
 */
  @Disabled("Long running benchmark (~16s)")
  @Test
  void vajram_benchmark_2() throws Exception {
    int outerLoopCount = 100;
    int innerLoopCount = 10000;

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
          graph.createExecutor(
              new FormulaRequestContext(100, 20, 5, "formulaTest")
              //
              /*   ,
              KrystalNodeExecutorConfig.builder()
                  .nodeExecStrategy(NodeExecStrategy.GRANULAR)
                  .dependencyExecStrategy(DependencyExecStrategy.INCREMENTAL)
                  .build()*/
              //
              )) {
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

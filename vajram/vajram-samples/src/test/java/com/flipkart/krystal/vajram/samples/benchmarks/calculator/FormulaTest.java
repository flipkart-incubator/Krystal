package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;
import static com.flipkart.krystal.vajram.samples.Util.javaMethodBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.printStats;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder.add;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.Divider.divide;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryon.KryonExecutorMetrics;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.modulation.Batcher;
import com.flipkart.krystal.vajram.samples.Util;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderRequest;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.DividerRequest;
import com.flipkart.krystal.vajramexecutor.krystex.InputModulatorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.Builder;
import com.flipkart.krystal.vajramexecutor.krystex.testharness.VajramTestHarness;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FormulaTest {

  private Builder graph;
  private static final String REQUEST_ID = "formulaTest";
  private static final String PACKAGE_PATH = "com.flipkart.krystal.vajram.samples.benchmarks.calculator";

  @BeforeEach
  void setUp() {
    graph = loadFromClasspath(PACKAGE_PATH);
    Adder.CALL_COUNTER.reset();
  }

  @Test
  void formula_success() throws Exception {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputModulators(
        vajramID(getVajramIdString(Adder.class)),
        InputModulatorConfig.simple(() -> new Batcher<>(100)));
    try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
        graph.createExecutor(new FormulaRequestContext(100, 20, 5, REQUEST_ID))) {
      future = executeVajram(krystexVajramExecutor, 0);
    }
    assertThat(future.get()).isEqualTo(4);
    assertThat(Adder.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @Disabled("Long running benchmark (~16s)")
  @Test
  void vajram_benchmark() throws Exception {
    int loopCount = 1_000_000;
    VajramKryonGraph graph = this.graph.maxParallelismPerCore(5).build();
    long javaNativeTimeNs = javaMethodBenchmark(FormulaTest::syncFormula, loopCount);
    long javaFuturesTimeNs = Util.javaFuturesBenchmark(FormulaTest::asyncFormula, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KryonExecutorMetrics[] metrics = new KryonExecutorMetrics[loopCount];
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    long startTime = System.nanoTime();
    for (int value = 0; value < loopCount; value++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
          graph.createExecutor(new FormulaRequestContext(100, 20, 5, "formulaTest"))) {
        timeToCreateExecutors += System.nanoTime() - iterStartTime;
        metrics[value] =
            ((KryonExecutor) krystexVajramExecutor.getKrystalExecutor()).getKryonMetrics();
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

  @Disabled("Long running benchmark (~16s)")
  @Test
  void vajram_benchmark_2() throws Exception {
    int outerLoopCount = 1000;
    int innerLoopCount = 1000;
    int loopCount = outerLoopCount * innerLoopCount;
    VajramKryonGraph graph = this.graph.maxParallelismPerCore(1).build();
    graph.registerInputModulators(
        vajramID(getVajramIdString(Adder.class)),
        InputModulatorConfig.simple(() -> new Batcher<>(innerLoopCount)));
    long javaNativeTimeNs = javaMethodBenchmark(FormulaTest::syncFormula, loopCount);
    long javaFuturesTimeNs = Util.javaFuturesBenchmark(FormulaTest::asyncFormula, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KryonExecutorMetrics[] metrics = new KryonExecutorMetrics[outerLoopCount];
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    long startTime = System.nanoTime();
    for (int outer_i = 0; outer_i < outerLoopCount; outer_i++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
          graph.createExecutor(new FormulaRequestContext(100, 20, 5, "formulaTest"))) {
        timeToCreateExecutors += System.nanoTime() - iterStartTime;
        metrics[outer_i] =
            ((KryonExecutor) krystexVajramExecutor.getKrystalExecutor()).getKryonMetrics();
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
        vajramID(getVajramIdString(Formula.class)),
        rc -> FormulaRequest.builder().a(rc.a + value).p(rc.p + value).q(rc.q + value).build(),
        KryonExecutionConfig.builder().executionId("formulaTest" + value).build());
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

  private record FormulaRequestContext(Integer a, Integer p, Integer q, String requestId)
      implements ApplicationRequestContext {

  }

  private static Builder loadFromClasspath(String... packagePrefixes) {
    Builder builder = VajramKryonGraph.builder();
    Arrays.stream(packagePrefixes).forEach(builder::loadFromPackage);
    return builder;
  }

  @Test
  void formula_success_withAllMockedDependencies() throws Exception {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputModulators(
        vajramID(getVajramIdString(Adder.class)),
        InputModulatorConfig.simple(() -> new Batcher<>(100)));
    KryonExecutorConfigBuilder kryonExecutorConfigBuilder = KryonExecutorConfig.builder()
        .kryonExecStrategy(
            KryonExecStrategy.BATCH).graphTraversalStrategy(GraphTraversalStrategy.DEPTH);
    try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
        graph.createExecutor(new FormulaRequestContext(100, 20, 5, REQUEST_ID),
            VajramTestHarness
                .prepareForTest(kryonExecutorConfigBuilder)
                .withMock(AdderRequest.builder().numberOne(20).numberTwo(5).build(),
                    ValueOrError.withValue(25))
                .withMock(
                    DividerRequest.builder().numerator(100).denominator(25).build(),
                    ValueOrError.withValue(4))
                .buildConfig())) {
      future = executeVajram(krystexVajramExecutor, 0);
    }
    assertThat(future.get()).isEqualTo(4);
    assertThat(Adder.CALL_COUNTER.sum()).isEqualTo(0);
  }

  @Test
  void formula_success_with_mockedDependencyAdder() throws Exception {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputModulators(
        vajramID(getVajramIdString(Adder.class)),
        InputModulatorConfig.simple(() -> new Batcher<>(100)));
    KryonExecutorConfigBuilder kryonExecutorConfigBuilder = KryonExecutorConfig.builder()
        .kryonExecStrategy(
            KryonExecStrategy.BATCH).graphTraversalStrategy(GraphTraversalStrategy.DEPTH);
    try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
        graph.createExecutor(new FormulaRequestContext(100, 20, 5, REQUEST_ID),
            VajramTestHarness
                .prepareForTest(kryonExecutorConfigBuilder)
                .withMock(AdderRequest.builder().numberOne(20).numberTwo(5).build(), ValueOrError.withValue(25))
                .buildConfig())) {
      future = executeVajram(krystexVajramExecutor, 0);
    }
    assertThat(future.get()).isEqualTo(4);
    assertThat(Adder.CALL_COUNTER.sum()).isEqualTo(0);
  }

  @Test
  void formula_success_with_mockedDependencyDivider() throws Exception {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputModulators(
        vajramID(getVajramIdString(Adder.class)),
        InputModulatorConfig.simple(() -> new Batcher<>(100)));
    KryonExecutorConfigBuilder kryonExecutorConfigBuilder = KryonExecutorConfig.builder()
        .kryonExecStrategy(
            KryonExecStrategy.BATCH).graphTraversalStrategy(GraphTraversalStrategy.DEPTH);
    try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
        graph.createExecutor(new FormulaRequestContext(100, 20, 5, REQUEST_ID),
            VajramTestHarness
                .prepareForTest(kryonExecutorConfigBuilder)
                .withMock(DividerRequest.builder().numerator(100).denominator(25).build(), ValueOrError.withValue(4))
                .buildConfig())) {
      future = executeVajram(krystexVajramExecutor, 0);
    }
    assertThat(future.get()).isEqualTo(4);
    assertThat(Adder.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @Test
  void formula_failure() throws Exception {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputModulators(
        vajramID(getVajramIdString(Adder.class)),
        InputModulatorConfig.simple(() -> new Batcher<>(100)));
    KryonExecutorConfigBuilder kryonExecutorConfigBuilder = KryonExecutorConfig.builder()
        .kryonExecStrategy(
            KryonExecStrategy.BATCH).graphTraversalStrategy(GraphTraversalStrategy.DEPTH);
    try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
        graph.createExecutor(new FormulaRequestContext(100, 0, 0, REQUEST_ID),
            VajramTestHarness
                .prepareForTest(kryonExecutorConfigBuilder)
                .withMock(AdderRequest.builder().numberOne(0).numberTwo(0).build(),
                    ValueOrError.withValue(0))
                .buildConfig())) {
      future = executeVajram(krystexVajramExecutor, 0);
      assertThat(future.isCompletedExceptionally()).isEqualTo(false);
    }
  }
}

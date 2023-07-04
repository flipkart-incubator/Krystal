package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.samples.Util.javaFuturesBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.javaMethodBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.printStats;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder.add;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.ChainAdderRequest.chainSum_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.ChainAdderRequest.sum_n;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.decorators.observability.DefaultNodeExecutionReport;
import com.flipkart.krystal.krystex.decorators.observability.MainLogicExecReporter;
import com.flipkart.krystal.krystex.decorators.observability.NodeExecutionReport;
import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutorConfig;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutorMetrics;
import com.flipkart.krystal.krystex.node.NodeExecutionConfig;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.modulation.Batcher;
import com.flipkart.krystal.vajramexecutor.krystex.InputModulatorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ChainAdderTest {
  private VajramNodeGraph graph;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    Adder.CALL_COUNTER.reset();
    graph =
        loadFromClasspath("com.flipkart.krystal.vajram.samples.benchmarks.calculator")
            .maxParallelismPerCore(1)
            .build();
    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .setSerializationInclusion(NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Test
  void chainer_success() throws Exception {
    CompletableFuture<Integer> future;
    NodeExecutionReport nodeExecutionReport = new DefaultNodeExecutionReport(Clock.systemUTC());
    MainLogicExecReporter mainLogicExecReporter = new MainLogicExecReporter(nodeExecutionReport);
    graph.registerInputModulators(
        vajramID(Adder.ID), InputModulatorConfig.simple(() -> new Batcher<>(100)));
    try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
        graph.createExecutor(
            new RequestContext("chainAdderTest"),
            KrystalNodeExecutorConfig.builder()
                .requestScopedLogicDecoratorConfigs(
                    ImmutableMap.of(
                        mainLogicExecReporter.decoratorType(),
                        List.of(
                            new MainLogicDecoratorConfig(
                                mainLogicExecReporter.decoratorType(),
                                logicExecutionContext -> true,
                                logicExecutionContext -> mainLogicExecReporter.decoratorType(),
                                decoratorContext -> mainLogicExecReporter))))
                .build())) {
      future = executeVajram(krystexVajramExecutor, 0);
    }
    assertThat(future.get()).isEqualTo(55);
    //    assertThat(Adder.CALL_COUNTER.sum()).isEqualTo(1);
    System.out.println(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(nodeExecutionReport));
  }

  @Disabled("Long running benchmark")
  @Test
  void vajram_benchmark() throws Exception {
    int loopCount = 50_000;
    long javaNativeTimeNs = javaMethodBenchmark(this::chainAdd, loopCount);
    long javaFuturesTimeNs = javaFuturesBenchmark(this::chainAddAsync, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KrystalNodeExecutorMetrics[] metrics = new KrystalNodeExecutorMetrics[loopCount];
    long startTime = System.nanoTime();
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    graph.registerInputModulators(
        vajramID(Adder.ID),
        InputModulatorConfig.sharedModulator(
            () -> new Batcher<>(100), "adderBatcher", getBatchedDepChains()));
    for (int value = 0; value < loopCount; value++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
          graph.createExecutor(new RequestContext("chainAdderTest"))) {
        metrics[value] =
            ((KrystalNodeExecutor) krystexVajramExecutor.getKrystalExecutor())
                .getKrystalNodeMetrics();
        timeToCreateExecutors += System.nanoTime() - iterStartTime;
        long enqueueStart = System.nanoTime();
        futures[value] = executeVajram(krystexVajramExecutor, value);
        timeToEnqueueVajram += System.nanoTime() - enqueueStart;
      }
    }
    allOf(futures).join();
    long vajramTimeNs = System.nanoTime() - startTime;
    /*
     * Benchmark config:
     *    loopCount = 50_000
     *    maxParallelismPerCore = 0.5
     *    Processor: 2.6 GHz 6-Core Intel Core i7
     * Best Benchmark result:
     *    platform overhead over reactive code = ~764 µs  per request
     *    maxPoolSize = 6
     *    maxActiveLeasesPerObject: 4085
     *    peakAvgActiveLeasesPerObject: 4083.25
     *    Avg. time to Enqueue vajrams: 28,183 ns
     *    Avg. time to execute vajrams: 765,979 ns
     *    Throughput executions/sec: 1315
     *    CommandsQueuedCount: 150,000
     *    CommandQueueBypassedCount: 9,950,000
     */
    allOf(futures)
        .whenComplete(
            (unused, throwable) -> {
              for (int i = 0; i < futures.length; i++) {
                CompletableFuture<Integer> future = futures[i];
                assertThat(future.getNow(0)).isEqualTo((i * 100) + 55);
              }
            })
        .get();
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

  @Disabled("Long running benchmark")
  @Test
  void vajram_benchmark_2() throws Exception {
    int outerLoopCount = 200;
    int innerLoopCount = 250;
    int loopCount = outerLoopCount * innerLoopCount;

    long javaNativeTimeNs = javaMethodBenchmark(this::chainAdd, loopCount);
    long javaFuturesTimeNs = javaFuturesBenchmark(this::chainAddAsync, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KrystalNodeExecutorMetrics[] metrics = new KrystalNodeExecutorMetrics[outerLoopCount];
    long startTime = System.nanoTime();
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    graph.registerInputModulators(
        vajramID(Adder.ID),
        InputModulatorConfig.sharedModulator(
            () -> new Batcher<>(100), "adderBatcher", getBatchedDepChains()));
    for (int outer_i = 0; outer_i < outerLoopCount; outer_i++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
          graph.createExecutor(new RequestContext("chainAdderTest"))) {
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
    /*
     * Benchmark config:
     *    loopCount = 50_000
     *    maxParallelismPerCore = 0.5
     *    Processor: 2.6 GHz 6-Core Intel Core i7
     * Best Benchmark result:
     *    platform overhead over reactive code = ~764 µs  per request
     *    maxPoolSize = 6
     *    maxActiveLeasesPerObject: 4085
     *    peakAvgActiveLeasesPerObject: 4083.25
     *    Avg. time to Enqueue vajrams: 28,183 ns
     *    Avg. time to execute vajrams: 765,979 ns
     *    Throughput executions/sec: 1315
     *    CommandsQueuedCount: 150,000
     *    CommandQueueBypassedCount: 9,950,000
     */
    allOf(futures)
        .whenComplete(
            (unused, throwable) -> {
              for (int i = 0; i < futures.length; i++) {
                CompletableFuture<Integer> future = futures[i];
                assertThat(future.getNow(0)).isEqualTo((i * 100) + 55);
              }
            })
        .get();
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

  private CompletableFuture<Integer> executeVajram(
      KrystexVajramExecutor<RequestContext> krystexVajramExecutor, int multiplier) {
    return krystexVajramExecutor.execute(
        vajramID(ChainAdder.ID),
        rc ->
            ChainAdderRequest.builder()
                .numbers(
                    new ArrayList<>(
                        Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                            .map(integer -> integer + multiplier * 10)
                            .toList()))
                .build(),
        NodeExecutionConfig.builder()
            .executionId(String.valueOf(multiplier))
            // Tests whether request level disabled dependant chains is working
            .disabledDependantChains(getDisabledDependantChains(graph))
            .build());
  }

  private void chainAdd(Integer value) {
    chainAdd(
        new ArrayList<>(
            Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .map(integer -> integer + value * 10)
                .toList()));
  }

  private int chainAdd(List<Integer> numbers) {
    if (numbers.isEmpty()) {
      return 0;
    } else if (numbers.size() == 1) {
      return add(numbers.get(0), 0);
    } else if (numbers.size() == 2) {
      return add(numbers.get(0), numbers.get(1));
    } else {
      return chainAdd(numbers.subList(0, numbers.size() - 1))
             + add(numbers.get(numbers.size() - 1), 0);
    }
  }

  private CompletableFuture<Integer> chainAddAsync(Integer value) {
    return chainAddAsync(
        new ArrayList<>(
            Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .map(integer -> integer + value * 10)
                .toList()));
  }

  private CompletableFuture<Integer> chainAddAsync(List<Integer> numbers) {
    if (numbers.isEmpty()) {
      return completedFuture(0);
    } else if (numbers.size() == 1) {
      return addAsync(numbers.get(0), 0);
    } else if (numbers.size() == 2) {
      return addAsync(numbers.get(0), numbers.get(1));
    } else {
      return chainAddAsync(numbers.subList(0, numbers.size() - 1))
          .thenCombine(addAsync(numbers.get(numbers.size() - 1), 0), Integer::sum);
    }
  }

  private CompletableFuture<Integer> addAsync(int a, int b) {
    return completedFuture(a + b);
  }

  record RequestContext(String requestId) implements ApplicationRequestContext {}

  private static Builder loadFromClasspath(String... packagePrefixes) {
    Builder builder = VajramNodeGraph.builder();
    Arrays.stream(packagePrefixes).forEach(builder::loadFromPackage);
    return builder;
  }

  private static ImmutableSet<DependantChain> getDisabledDependantChains(VajramNodeGraph graph) {
    return ImmutableSet.of(
        graph.computeDependantChain(
            ChainAdder.ID,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n));
  }

  private DependantChain[] getBatchedDepChains() {
    return new DependantChain[] {
        graph.computeDependantChain(ChainAdder.ID, sum_n),
        graph.computeDependantChain(ChainAdder.ID, chainSum_n, sum_n),
        graph.computeDependantChain(ChainAdder.ID, chainSum_n, chainSum_n, sum_n),
        graph.computeDependantChain(ChainAdder.ID, chainSum_n, chainSum_n, chainSum_n, sum_n),
        graph.computeDependantChain(
            ChainAdder.ID, chainSum_n, chainSum_n, chainSum_n, chainSum_n, sum_n),
        graph.computeDependantChain(
            ChainAdder.ID, chainSum_n, chainSum_n, chainSum_n, chainSum_n, chainSum_n, sum_n),
        graph.computeDependantChain(
            ChainAdder.ID,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            sum_n),
        graph.computeDependantChain(
            ChainAdder.ID,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            sum_n),
        graph.computeDependantChain(
            ChainAdder.ID,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            chainSum_n,
            sum_n)
    };
  }
}

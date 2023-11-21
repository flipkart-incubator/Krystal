package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;
import static com.flipkart.krystal.vajram.samples.Util.javaFuturesBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.javaMethodBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.printStats;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder.add;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.SplitAdderRequest.splitSum1_n;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.SplitAdderRequest.splitSum2_n;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorMetrics;
import com.flipkart.krystal.krystex.logicdecoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecorators.observability.DefaultKryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.KryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.MainLogicExecReporter;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.ChainAdderTest.RequestContext;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.Builder;
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

class SplitAdderTest {
  private VajramKryonGraph graph;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
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
  void splitAdder_success() throws Exception {
    CompletableFuture<Integer> future;
    KryonExecutionReport kryonExecutionReport = new DefaultKryonExecutionReport(Clock.systemUTC());
    MainLogicExecReporter mainLogicExecReporter = new MainLogicExecReporter(kryonExecutionReport);
    try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
        graph.createExecutor(
            new RequestContext("chainAdderTest"),
            KryonExecutorConfig.builder()
                .requestScopedLogicDecoratorConfigs(
                    ImmutableMap.of(
                        mainLogicExecReporter.decoratorType(),
                        List.of(
                            new MainLogicDecoratorConfig(
                                mainLogicExecReporter.decoratorType(),
                                logicExecutionContext -> true,
                                logicExecutionContext -> mainLogicExecReporter.decoratorType(),
                                decoratorContext -> mainLogicExecReporter))))
                // Tests whether instasnce level disabled dependant chains is working
                .disabledDependantChains(disabledDepChains(graph))
                .build())) {
      future = executeVajram(krystexVajramExecutor, 0);
    }
    assertThat(future).succeedsWithin(ofSeconds(1)).isEqualTo(55);
    System.out.println(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kryonExecutionReport));
  }

  @Disabled("Long running benchmark")
  @Test
  void vajram_benchmark() throws Exception {
    int loopCount = 50_000;
    long javaNativeTimeNs = javaMethodBenchmark(this::splitAdd, loopCount);
    long javaFuturesTimeNs = javaFuturesBenchmark(this::splitAddAsync, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KryonExecutorMetrics[] metrics = new KryonExecutorMetrics[loopCount];
    long startTime = System.nanoTime();
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    for (int value = 0; value < loopCount; value++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
          graph.createExecutor(
              new RequestContext("splitAdderTest"),
              KryonExecutorConfig.builder()
                  .disabledDependantChains(disabledDepChains(graph))
                  .build())) {
        metrics[value] =
            ((KryonExecutor) krystexVajramExecutor.getKrystalExecutor()).getKryonMetrics();
        timeToCreateExecutors += System.nanoTime() - iterStartTime;
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
              for (int i = 0; i < futures.length; i++) {
                CompletableFuture<Integer> future = futures[i];
                assertThat(future.getNow(0)).isEqualTo((i * 100) + 55);
              }
            })
        .get();
    /*
     * Benchmark config:
     *    loopCount = 50_000
     *    maxParallelismPerCore = 0.5
     *    Processor: 2.6 GHz 6-Core Intel Core i7 (with hyperthreading - 12 virtual cores)
     * Best Benchmark result:
     *    platform overhead = ~641 µs per request
     *    maxPoolSize = 6
     *    maxActiveLeasesPerObject: 8094
     *    peakAvgActiveLeasesPerObject: 8091.67
     *    Avg. time to Enqueue vajrams: 13,534 ns
     *    Avg. time to execute vajrams: 643,995 ns
     *    Throughput executions/sec: 1562
     *    CommandsQueuedCount: 150,000
     *    CommandQueueBypassedCount: 9,650,000
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

  @Disabled("Long running benchmark")
  @Test
  void vajram_benchmark_2() throws Exception {
    int outerLoopCount = 100;
    int innerLoopCount = 500;
    int loopCount = outerLoopCount * innerLoopCount;

    long javaNativeTimeNs = javaMethodBenchmark(this::splitAdd, loopCount);
    long javaFuturesTimeNs = javaFuturesBenchmark(this::splitAddAsync, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KryonExecutorMetrics[] metrics = new KryonExecutorMetrics[outerLoopCount];
    long startTime = System.nanoTime();
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    for (int outer_i = 0; outer_i < outerLoopCount; outer_i++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
          graph.createExecutor(
              new RequestContext("splitAdderTest"),
              KryonExecutorConfig.builder()
                  .disabledDependantChains(disabledDepChains(graph))
                  .build())) {
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

  private static CompletableFuture<Integer> executeVajram(
      KrystexVajramExecutor<RequestContext> krystexVajramExecutor, int multiplier) {
    return krystexVajramExecutor.execute(
        vajramID(getVajramIdString(SplitAdder.class)),
        rc ->
            SplitAdderRequest.builder()
                .numbers(
                    new ArrayList<>(
                        Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                            .map(integer -> integer + multiplier * 10)
                            .toList()))
                .build(),
        KryonExecutionConfig.builder().executionId(String.valueOf(multiplier)).build());
  }

  private void splitAdd(Integer value) {
    splitAdd(
        new ArrayList<>(
            Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .map(integer -> integer + value * 10)
                .toList()));
  }

  private int splitAdd(List<Integer> numbers) {
    if (numbers.isEmpty()) {
      return 0;
    } else if (numbers.size() == 1) {
      return add(numbers.get(0), 0);
    } else {
      int subListSize = numbers.size() / 2;
      return splitAdd(numbers.subList(0, subListSize))
          + splitAdd(numbers.subList(subListSize, numbers.size()));
    }
  }

  private CompletableFuture<Integer> splitAddAsync(Integer value) {
    return splitAddAsync(
        new ArrayList<>(
            Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .map(integer -> integer + value * 10)
                .toList()));
  }

  private CompletableFuture<Integer> splitAddAsync(List<Integer> numbers) {
    if (numbers.isEmpty()) {
      return completedFuture(0);
    } else if (numbers.size() == 1) {
      return addAsync(numbers.get(0), 0);
    } else {
      int subListSize = numbers.size() / 2;
      return splitAddAsync(numbers.subList(0, subListSize))
          .thenCombine(splitAddAsync(numbers.subList(subListSize, numbers.size())), Integer::sum);
    }
  }

  private CompletableFuture<Integer> addAsync(int a, int b) {
    return completedFuture(a + b);
  }

  private static Builder loadFromClasspath(String... packagePrefixes) {
    Builder builder = VajramKryonGraph.builder();
    Arrays.stream(packagePrefixes).forEach(builder::loadFromPackage);
    return builder;
  }

  private static ImmutableSet<DependantChain> disabledDepChains(VajramKryonGraph graph) {
    String splitAdderId = getVajramIdString(SplitAdder.class);
    return ImmutableSet.of(
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum1_n, splitSum1_n, splitSum1_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum1_n, splitSum1_n, splitSum1_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum1_n, splitSum1_n, splitSum2_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum1_n, splitSum1_n, splitSum2_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum1_n, splitSum2_n, splitSum1_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum1_n, splitSum2_n, splitSum1_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum1_n, splitSum2_n, splitSum2_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum1_n, splitSum2_n, splitSum2_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum2_n, splitSum1_n, splitSum1_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum2_n, splitSum1_n, splitSum1_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum2_n, splitSum1_n, splitSum2_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum2_n, splitSum1_n, splitSum2_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum2_n, splitSum2_n, splitSum1_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum2_n, splitSum2_n, splitSum1_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum2_n, splitSum2_n, splitSum2_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum1_n, splitSum2_n, splitSum2_n, splitSum2_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum1_n, splitSum1_n, splitSum1_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum1_n, splitSum1_n, splitSum1_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum1_n, splitSum1_n, splitSum2_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum1_n, splitSum1_n, splitSum2_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum1_n, splitSum2_n, splitSum1_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum1_n, splitSum2_n, splitSum1_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum1_n, splitSum2_n, splitSum2_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum1_n, splitSum2_n, splitSum2_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum2_n, splitSum1_n, splitSum1_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum2_n, splitSum1_n, splitSum1_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum2_n, splitSum1_n, splitSum2_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum2_n, splitSum1_n, splitSum2_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum2_n, splitSum2_n, splitSum1_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum2_n, splitSum2_n, splitSum1_n, splitSum2_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum2_n, splitSum2_n, splitSum2_n, splitSum1_n),
        graph.computeDependantChain(
            splitAdderId, splitSum2_n, splitSum2_n, splitSum2_n, splitSum2_n, splitSum2_n));
  }
}

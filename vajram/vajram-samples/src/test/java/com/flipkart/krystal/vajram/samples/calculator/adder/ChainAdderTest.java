package com.flipkart.krystal.vajram.samples.calculator.adder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.vajram.VajramID.ofVajram;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;
import static com.flipkart.krystal.vajram.samples.Util.javaFuturesBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.javaMethodBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.printStats;
import static com.flipkart.krystal.vajram.samples.calculator.adder.Adder.add;
import static com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdderFacets.chainSum_s;
import static com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdderFacets.sum_s;
import static com.flipkart.krystal.vajram.samples.calculator.adder.ChainAdderRequest._builder;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
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
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecorators.observability.DefaultKryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.KryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.MainLogicExecReporter;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.flipkart.krystal.vajram.samples.calculator.Formula;
import com.flipkart.krystal.vajramexecutor.krystex.InputBatcherConfig;
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

class ChainAdderTest {
  private VajramKryonGraph graph;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    Adder.CALL_COUNTER.reset();
    graph = loadFromClasspath(Formula.class.getPackageName()).maxParallelismPerCore(1).build();
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
    KryonExecutionReport kryonExecutionReport = new DefaultKryonExecutionReport(Clock.systemUTC());
    MainLogicExecReporter mainLogicExecReporter = new MainLogicExecReporter(kryonExecutionReport);
    graph.registerInputBatchers(
        vajramID(getVajramIdString(Adder.class)),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl(100), "adderBatcher", getBatchedDepChains()));
    try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
        graph.createExecutor(
            new RequestContext("chainAdderTest"),
            KryonExecutorConfig.builder()
                .requestScopedLogicDecoratorConfigs(
                    ImmutableMap.of(
                        mainLogicExecReporter.decoratorType(),
                        List.of(
                            new OutputLogicDecoratorConfig(
                                mainLogicExecReporter.decoratorType(),
                                logicExecutionContext -> true,
                                logicExecutionContext -> mainLogicExecReporter.decoratorType(),
                                decoratorContext -> mainLogicExecReporter))))
                .build())) {
      future = executeVajram(krystexVajramExecutor, 0);
    }
    assertThat(future).succeedsWithin(ofSeconds(1000)).isEqualTo(55);
    assertThat(Adder.CALL_COUNTER.sum()).isEqualTo(1);
    System.out.println(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kryonExecutionReport));
  }

  @Test
  void emptyNumbers_returnsZero_success() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
        graph.createExecutor(new RequestContext("chainAdderTest"))) {
      future =
          krystexVajramExecutor.execute(
              ofVajram(ChainAdder.class),
              rc -> ChainAdderRequest._builder().numbers(List.of())._build(),
              KryonExecutionConfig.builder()
                  .disabledDependantChains(getDisabledDependantChains(graph))
                  .build());
    }
    assertThat(future).succeedsWithin(1, SECONDS).isEqualTo(0);
  }

  @Disabled("Long running benchmark")
  @Test
  void vajram_benchmark() throws Exception {
    int loopCount = 50_000;
    long javaNativeTimeNs = javaMethodBenchmark(this::chainAdd, loopCount);
    long javaFuturesTimeNs = javaFuturesBenchmark(this::chainAddAsync, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KryonExecutorMetrics[] metrics = new KryonExecutorMetrics[loopCount];
    long startTime = System.nanoTime();
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    graph.registerInputBatchers(
        vajramID(getVajramIdString(Adder.class)),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl(100), "adderBatcher", getBatchedDepChains()));
    for (int value = 0; value < loopCount; value++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
          graph.createExecutor(new RequestContext("chainAdderTest"))) {
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
    int outerLoopCount = 100;
    int innerLoopCount = 500;
    int loopCount = outerLoopCount * innerLoopCount;

    long javaNativeTimeNs = javaMethodBenchmark(this::chainAdd, loopCount);
    long javaFuturesTimeNs = javaFuturesBenchmark(this::chainAddAsync, loopCount);
    //noinspection unchecked
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KryonExecutorMetrics[] metrics = new KryonExecutorMetrics[outerLoopCount];
    long startTime = System.nanoTime();
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    graph.registerInputBatchers(
        vajramID(getVajramIdString(Adder.class)),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl(100), "adderBatcher", getBatchedDepChains()));
    for (int outer_i = 0; outer_i < outerLoopCount; outer_i++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
          graph.createExecutor(new RequestContext("chainAdderTest"))) {
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

  private CompletableFuture<Integer> executeVajram(
      KrystexVajramExecutor<RequestContext> krystexVajramExecutor, int multiplier) {
    return krystexVajramExecutor.execute(
        vajramID(getVajramIdString(ChainAdder.class)),
        rc ->
            _builder()
                .numbers(
                    new ArrayList<>(
                        Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                            .map(integer -> integer + multiplier * 10)
                            .toList()))
                ._build(),
        KryonExecutionConfig.builder()
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
    Builder builder = VajramKryonGraph.builder();
    Arrays.stream(packagePrefixes).forEach(builder::loadFromPackage);
    return builder;
  }

  private static ImmutableSet<DependantChain> getDisabledDependantChains(VajramKryonGraph graph) {
    return ImmutableSet.of(
        graph.computeDependantChain(
            getVajramIdString(ChainAdder.class),
            chainSum_s.id(),
            chainSum_s.id(),
            chainSum_s.id(),
            chainSum_s.id(),
            chainSum_s.id(),
            chainSum_s.id(),
            chainSum_s.id(),
            chainSum_s.id(),
            chainSum_s.id()));
  }

  private DependantChain[] getBatchedDepChains() {
    String chainAdderId = getVajramIdString(ChainAdder.class);
    return new DependantChain[] {
      graph.computeDependantChain(chainAdderId, sum_s.id()),
      graph.computeDependantChain(chainAdderId, chainSum_s.id(), sum_s.id()),
      graph.computeDependantChain(chainAdderId, chainSum_s.id(), chainSum_s.id(), sum_s.id()),
      graph.computeDependantChain(chainAdderId, chainSum_s.id(), chainSum_s.id(), chainSum_s.id(), sum_s.id()),
      graph.computeDependantChain(
          chainAdderId, chainSum_s.id(), chainSum_s.id(), chainSum_s.id(), chainSum_s.id(), sum_s.id()),
      graph.computeDependantChain(
          chainAdderId, chainSum_s.id(), chainSum_s.id(), chainSum_s.id(), chainSum_s.id(), chainSum_s.id(), sum_s.id()),
      graph.computeDependantChain(
          chainAdderId,
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          sum_s.id()),
      graph.computeDependantChain(
          chainAdderId,
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          sum_s.id()),
      graph.computeDependantChain(
          chainAdderId,
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          chainSum_s.id(),
          sum_s.id())
    };
  }
}

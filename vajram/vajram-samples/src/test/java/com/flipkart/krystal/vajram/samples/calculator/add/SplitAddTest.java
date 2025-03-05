package com.flipkart.krystal.vajram.samples.calculator.add;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.vajram.samples.Util.javaFuturesBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.javaMethodBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.printStats;
import static com.flipkart.krystal.vajram.samples.calculator.add.Add.add;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum1_s;
import static com.flipkart.krystal.vajram.samples.calculator.add.SplitAdd_Fac.splitSum2_s;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorMetrics;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecorators.observability.DefaultKryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.KryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.MainLogicExecReporter;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.samples.calculator.Formula;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SplitAddTest {

  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 4);
  }

  private VajramKryonGraph graph;
  private ObjectMapper objectMapper;
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    graph = loadFromClasspath(Formula.class.getPackageName()).build();
    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .setSerializationInclusion(NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void splitAdder_success() throws Exception {
    CompletableFuture<Integer> future;
    KryonExecutionReport kryonExecutionReport = new DefaultKryonExecutionReport(Clock.systemUTC());
    MainLogicExecReporter mainLogicExecReporter = new MainLogicExecReporter(kryonExecutionReport);
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .requestId("chainAdderTest")
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .singleThreadExecutor(executorLease.get())
                        .requestScopedLogicDecoratorConfigs(
                            ImmutableMap.of(
                                mainLogicExecReporter.decoratorType(),
                                List.of(
                                    new OutputLogicDecoratorConfig(
                                        mainLogicExecReporter.decoratorType(),
                                        logicExecutionContext -> true,
                                        logicExecutionContext ->
                                            mainLogicExecReporter.decoratorType(),
                                        decoratorContext -> mainLogicExecReporter))))
                        // Tests whether executor level disabled dependant chains is working
                        .disabledDependantChains(disabledDepChains(graph)))
                .build())) {
      future = executeVajram(graph, krystexVajramExecutor, 0);
    }
    assertThat(future).succeedsWithin(ofSeconds(1)).isEqualTo(55);
    System.out.println(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kryonExecutionReport));
  }

  @Test
  void emptyNumbers_returnsZero_success() {
    CompletableFuture<Integer> future;
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .requestId("splitAdderTest")
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()))
                .build())) {
      future =
          krystexVajramExecutor.execute(
              graph.getVajramId(SplitAdd.class),
              SplitAdd_ImmutReqPojo._builder().numbers(List.of())._build(),
              KryonExecutionConfig.builder()
                  .disabledDependantChains(disabledDepChains(graph))
                  .build());
    }
    assertThat(future).succeedsWithin(1, SECONDS).isEqualTo(0);
  }

  @Disabled("Long running benchmark")
  @Test
  void vajram_benchmark() throws Exception {
    int loopCount = 50_000;
    long javaNativeTimeNs = javaMethodBenchmark(this::splitAdd, loopCount);
    long javaFuturesTimeNs = javaFuturesBenchmark(this::splitAddAsync, loopCount);
    @SuppressWarnings("unchecked")
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KryonExecutorMetrics[] metrics = new KryonExecutorMetrics[loopCount];
    long startTime = System.nanoTime();
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    for (int value = 0; value < loopCount; value++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor krystexVajramExecutor =
          graph.createExecutor(
              KrystexVajramExecutorConfig.builder()
                  .requestId("splitAdderTest")
                  .kryonExecutorConfigBuilder(
                      KryonExecutorConfig.builder()
                          .singleThreadExecutor(executorLease.get())
                          .disabledDependantChains(disabledDepChains(graph)))
                  .build())) {
        metrics[value] =
            ((KryonExecutor) krystexVajramExecutor.getKrystalExecutor()).getKryonMetrics();
        timeToCreateExecutors += System.nanoTime() - iterStartTime;
        long enqueueStart = System.nanoTime();
        futures[value] = executeVajram(graph, krystexVajramExecutor, value);
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
        javaNativeTimeNs,
        javaFuturesTimeNs,
        metrics,
        timeToCreateExecutors,
        timeToEnqueueVajram,
        vajramTimeNs,
        EXEC_POOL);
  }

  @Disabled("Long running benchmark")
  @Test
  void vajram_benchmark_2() throws Exception {
    int outerLoopCount = 100;
    int innerLoopCount = 500;
    int loopCount = outerLoopCount * innerLoopCount;

    long javaNativeTimeNs = javaMethodBenchmark(this::splitAdd, loopCount);
    long javaFuturesTimeNs = javaFuturesBenchmark(this::splitAddAsync, loopCount);
    @SuppressWarnings("unchecked")
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KryonExecutorMetrics[] metrics = new KryonExecutorMetrics[outerLoopCount];
    long startTime = System.nanoTime();
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    for (int outer_i = 0; outer_i < outerLoopCount; outer_i++) {
      long iterStartTime = System.nanoTime();
      try (KrystexVajramExecutor krystexVajramExecutor =
          graph.createExecutor(
              KrystexVajramExecutorConfig.builder()
                  .requestId("splitAdderTest")
                  .kryonExecutorConfigBuilder(
                      KryonExecutorConfig.builder()
                          .singleThreadExecutor(executorLease.get())
                          .disabledDependantChains(disabledDepChains(graph)))
                  .build())) {
        timeToCreateExecutors += System.nanoTime() - iterStartTime;
        metrics[outer_i] =
            ((KryonExecutor) krystexVajramExecutor.getKrystalExecutor()).getKryonMetrics();
        for (int inner_i = 0; inner_i < innerLoopCount; inner_i++) {
          int iterationNum = outer_i * innerLoopCount + inner_i;
          long enqueueStart = System.nanoTime();
          futures[iterationNum] = executeVajram(graph, krystexVajramExecutor, iterationNum);
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
        javaNativeTimeNs,
        javaFuturesTimeNs,
        metrics,
        timeToCreateExecutors,
        timeToEnqueueVajram,
        vajramTimeNs,
        EXEC_POOL);
  }

  private static CompletableFuture<Integer> executeVajram(
      VajramKryonGraph graph, KrystexVajramExecutor krystexVajramExecutor, int multiplier) {
    return krystexVajramExecutor.execute(
        graph.getVajramId(SplitAdd.class),
        SplitAdd_ImmutReqPojo._builder()
            .numbers(
                new ArrayList<>(
                    Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                        .map(integer -> integer + multiplier * 10)
                        .toList()))
            ._build(),
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

  private static VajramKryonGraphBuilder loadFromClasspath(String... packagePrefixes) {
    VajramKryonGraphBuilder builder = VajramKryonGraph.builder();
    Arrays.stream(packagePrefixes).forEach(builder::loadFromPackage);
    return builder;
  }

  private static ImmutableSet<DependantChain> disabledDepChains(VajramKryonGraph graph) {
    String splitAdderId = graph.getVajramId(SplitAdd.class).vajramId();
    return ImmutableSet.of(
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum1_s, splitSum1_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum1_s, splitSum1_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum1_s, splitSum2_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum1_s, splitSum2_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum2_s, splitSum1_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum2_s, splitSum1_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum2_s, splitSum2_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum1_s, splitSum2_s, splitSum2_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum1_s, splitSum1_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum1_s, splitSum1_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum1_s, splitSum2_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum1_s, splitSum2_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum2_s, splitSum1_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum2_s, splitSum1_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum2_s, splitSum2_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum1_s, splitSum2_s, splitSum2_s, splitSum2_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum1_s, splitSum1_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum1_s, splitSum1_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum1_s, splitSum2_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum1_s, splitSum2_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum2_s, splitSum1_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum2_s, splitSum1_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum2_s, splitSum2_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum1_s, splitSum2_s, splitSum2_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum1_s, splitSum1_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum1_s, splitSum1_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum1_s, splitSum2_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum1_s, splitSum2_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum2_s, splitSum1_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum2_s, splitSum1_s, splitSum2_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum2_s, splitSum2_s, splitSum1_s),
        graph.computeDependantChain(
            splitAdderId, splitSum2_s, splitSum2_s, splitSum2_s, splitSum2_s, splitSum2_s));
  }
}

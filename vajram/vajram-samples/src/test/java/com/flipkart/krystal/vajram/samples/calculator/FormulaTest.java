package com.flipkart.krystal.vajram.samples.calculator;

import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.DIRECT;
import static com.flipkart.krystal.vajram.samples.Util.TEST_TIMEOUT;
import static com.flipkart.krystal.vajram.samples.Util.javaMethodBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.printStats;
import static com.flipkart.krystal.vajram.samples.calculator.add.Add.FAIL_ADDER_FLAG;
import static com.flipkart.krystal.vajram.samples.calculator.add.Add.add;
import static com.flipkart.krystal.vajram.samples.calculator.divide.Divide.divide;
import static com.flipkart.krystal.vajramexecutor.krystex.batching.DepChainBatcherConfig.simple;
import static com.flipkart.krystal.vajramexecutor.krystex.testharness.VajramTestHarness.prepareForTest;
import static com.google.inject.Guice.createInjector;
import static com.google.inject.name.Names.named;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.stream;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.caching.TestRequestLevelCache;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorMetrics;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.flipkart.krystal.vajram.exception.MandatoryFacetsMissingException;
import com.flipkart.krystal.vajram.guice.inputinjection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajram.samples.Util;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import com.flipkart.krystal.vajram.samples.calculator.add.Add_FacImmutPojo;
import com.flipkart.krystal.vajram.samples.calculator.divide.Divide_FacImmutPojo;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.batching.InputBatcherConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class FormulaTest {

  public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

  @SuppressWarnings("unchecked")
  private static final Lease<SingleThreadExecutor>[] EXECUTOR_LEASES = new Lease[MAX_THREADS];

  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() throws LeaseUnavailableException {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", MAX_THREADS);
    for (int i = 0; i < MAX_THREADS; i++) {
      EXECUTOR_LEASES[i] = EXEC_POOL.lease();
    }
  }

  @AfterAll
  static void afterAll() {
    stream(EXECUTOR_LEASES).forEach(Lease::close);
  }

  private VajramKryonGraphBuilder graph;
  private static final String REQUEST_ID = "formulaTest";
  private final TestRequestLevelCache requestLevelCache = spy(new TestRequestLevelCache());

  private Lease<SingleThreadExecutor> executorLease;

  public static Stream<KryonExecStrategy> kryonExecStrategies() {
    return Collections.nCopies(20, DIRECT).stream();
  }

  @BeforeEach
  void setUp() {
    this.executorLease = EXECUTOR_LEASES[0];
    this.graph = Util.loadFromClasspath(Formula.class.getPackageName());
    Add.CALL_COUNTER.reset();
  }

  @ParameterizedTest
  @EnumSource(KryonExecStrategy.class)
  void formula_success(KryonExecStrategy kryonExecStrategy) {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputBatchers(
        simpleInputBatcher(
            graph.getVajramIdByVajramDefType(Add.class), () -> new InputBatcherImpl(100)));
    FormulaRequestContext requestContext = new FormulaRequestContext(100, 20, 5, REQUEST_ID);
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .executorId(REQUEST_ID)
                        .kryonExecStrategy(kryonExecStrategy)
                        .executorService(executorLease.get()))
                .build())) {
      future = executeVajram(krystexVajramExecutor, 0, requestContext);
    }
    assertThat(future).succeedsWithin(1, HOURS).isEqualTo(4);
    assertThat(Add.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @Test
  void formula_computeDepFails_failsWithException() {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputBatchers(
        simpleInputBatcher(
            graph.getVajramIdByVajramDefType(Add.class), () -> new InputBatcherImpl(100)));
    KrystexVajramExecutorConfig vajramExecutorConfig =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId(REQUEST_ID)
                    .kryonExecStrategy(BATCH)
                    .executorService(executorLease.get())
                    .graphTraversalStrategy(GraphTraversalStrategy.DEPTH))
            .build();
    FormulaRequestContext requestContext = new FormulaRequestContext(100, 0, 0, REQUEST_ID);
    when(requestLevelCache.getValue(any(Add_FacImmutPojo.class))).thenReturn(0);
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            prepareForTest(vajramExecutorConfig, requestLevelCache).buildConfig())) {
      future = executeVajram(krystexVajramExecutor, 0, requestContext);
    }
    assertThat(future)
        .failsWithin(ofSeconds(1))
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(ArithmeticException.class)
        .withMessage("java.lang.ArithmeticException: / by zero");
  }

  @Test
  void formula_ioDepFails_failsWithSameException() {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputInjector(injectAdderFailure());
    graph.registerInputBatchers(
        simpleInputBatcher(
            graph.getVajramIdByVajramDefType(Add.class), () -> new InputBatcherImpl(100)));
    FormulaRequestContext requestContext = new FormulaRequestContext(100, 20, 5, REQUEST_ID);
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .executorId(REQUEST_ID)
                        .executorService(executorLease.get()))
                .build())) {
      future = executeVajram(krystexVajramExecutor, 0, requestContext);
    }
    assertThat(future)
        .failsWithin(TEST_TIMEOUT)
        .withThrowableOfType(ExecutionException.class)
        .havingCause()
        .isInstanceOf(MandatoryFacetsMissingException.class)
        .withMessageContaining("Adder failed because fail flag was set");
  }

  /*
   * This test case is designed to catch race condition issues in graph loading
   * (Ex: https://github.com/flipkart-incubator/Krystal/issues/328)
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 4, 6, 8}) // test with different values of parallelism
  void parallelExecuteVajrams_success(int parallelism) {
    // This number must be divisible by parallelism. Else this test case will fail because we
    // won't be able to cleanly divide this total executionsCount equally to the executors.
    int executionsCount = 216;
    SingleThreadExecutor[] executors = getExecutors(parallelism);
    VajramKryonGraph graph = this.graph.build();
    CompletableFuture<?>[] submissionFutures = new CompletableFuture[parallelism];
    @SuppressWarnings("unchecked")
    CompletableFuture<Integer>[] futures = new CompletableFuture[executionsCount];
    LongAdder timeToCreateExecutors = new LongAdder();
    LongAdder timeToEnqueueVajram = new LongAdder();
    int loopCountPerExecutor = executionsCount / parallelism;

    for (int currentThread : range(0, parallelism).toArray()) {
      SingleThreadExecutor executor = executors[currentThread];
      int coreCountStart = currentThread * loopCountPerExecutor;
      submissionFutures[currentThread] =
          runAsync(
              () -> {
                FormulaRequestContext requestContext =
                    new FormulaRequestContext(100, 20, 5, "formulaTest");
                for (int currentLoopCount :
                    range(coreCountStart, coreCountStart + loopCountPerExecutor).toArray()) {
                  long iterStartTime = System.nanoTime();
                  try (KrystexVajramExecutor krystexVajramExecutor =
                      graph.createExecutor(
                          KrystexVajramExecutorConfig.builder()
                              .kryonExecutorConfigBuilder(
                                  KryonExecutorConfig.builder()
                                      .executorId("formulaTest")
                                      .executorService(executor))
                              .build())) {
                    timeToCreateExecutors.add(System.nanoTime() - iterStartTime);
                    long enqueueStart = System.nanoTime();
                    futures[currentLoopCount] =
                        executeVajram(krystexVajramExecutor, currentLoopCount, requestContext);
                    timeToEnqueueVajram.add(System.nanoTime() - enqueueStart);
                  }
                }
              },
              executor);
    }
    allOf(submissionFutures).join();
    allOf(futures).join();
    assertThat(
            allOf(futures)
                .whenComplete(
                    (unused, throwable) -> {
                      for (int i = 0, futuresLength = futures.length; i < futuresLength; i++) {
                        CompletableFuture<Integer> future = futures[i];
                        assertThat(future.getNow(0)).isEqualTo((100 + i) / (20 + i + 5 + i));
                      }
                    }))
        .succeedsWithin(ofSeconds(1));
    assertThat(Add.CALL_COUNTER.sum()).isEqualTo(executionsCount);
  }

  // Approx Latencies:
  // 1 core: ~4sec, 2 cores: ~6sec, 4 cores: ~6sec, 5 cores: ~7sec, 10 cores: ~6sec
  @Disabled("Long running benchmark")
  @ParameterizedTest
  @ValueSource(ints = {1, 1, 1, 1, 2, 4, 5, 10}) // test with different values of parallelism
  void millionExecutors_oneCallEach_NExecutors_benchmark(int executorCount) throws Exception {
    // This number must be divisible by executorCount. Else this test case will fail because we
    // won't be able to cleanly divide this total loopCount equally to the executors.
    int loopCount = 1_000_000;

    SingleThreadExecutor[] executors = getExecutors(executorCount);
    VajramKryonGraph graph = this.graph.build();
    long javaNativeTimeNs = javaMethodBenchmark(FormulaTest::syncFormula, loopCount);
    long javaFuturesTimeNs = Util.javaFuturesBenchmark(FormulaTest::asyncFormula, loopCount);
    CompletableFuture<?>[] submissionFutures = new CompletableFuture[executorCount];
    @SuppressWarnings("unchecked")
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KryonExecutorMetrics[] metrics = new KryonExecutorMetrics[loopCount];
    LongAdder timeToCreateExecutors = new LongAdder();
    LongAdder timeToEnqueueVajram = new LongAdder();
    long startTime = System.nanoTime();
    int loopCountPerExecutor = loopCount / executorCount;
    for (int currentExecutor : range(0, executorCount).toArray()) {
      SingleThreadExecutor executor = executors[currentExecutor];
      int coreCountStart = currentExecutor * loopCountPerExecutor;
      submissionFutures[currentExecutor] =
          runAsync(
              () -> {
                FormulaRequestContext requestContext =
                    new FormulaRequestContext(100, 20, 5, "formulaTest");
                for (int currentLoopCount :
                    range(coreCountStart, coreCountStart + loopCountPerExecutor).toArray()) {
                  long iterStartTime = System.nanoTime();
                  try (KrystexVajramExecutor krystexVajramExecutor =
                      graph.createExecutor(
                          KrystexVajramExecutorConfig.builder()
                              .kryonExecutorConfigBuilder(
                                  KryonExecutorConfig.builder()
                                      .executorId("formulaTest")
                                      .executorService(executor)
                                      .kryonExecStrategy(DIRECT))
                              .build())) {
                    timeToCreateExecutors.add(System.nanoTime() - iterStartTime);
                    metrics[currentLoopCount] =
                        ((KryonExecutor) krystexVajramExecutor.getKrystalExecutor())
                            .getKryonMetrics();
                    long enqueueStart = System.nanoTime();
                    futures[currentLoopCount] =
                        executeVajram(krystexVajramExecutor, currentLoopCount, requestContext);
                    timeToEnqueueVajram.add(System.nanoTime() - enqueueStart);
                  }
                }
              },
              executor);
    }
    allOf(submissionFutures).join();
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
        .succeedsWithin(ofSeconds(1));
    assertThat(Add.CALL_COUNTER.sum()).isEqualTo(loopCount);

    printStats(
        loopCount,
        1,
        javaNativeTimeNs,
        javaFuturesTimeNs,
        metrics,
        timeToCreateExecutors.sum(),
        timeToEnqueueVajram.sum(),
        vajramTimeNs,
        EXEC_POOL);
  }

  @Disabled("Long running benchmark (~2.5s)")
  @RepeatedTest(6)
  void thousandExecutors_1000CallsEach_singleCore_benchmark() throws Exception {
    SingleThreadExecutor executor = getExecutors(1)[0];
    int outerLoopCount = 1000;
    int innerLoopCount = 1000;
    int loopCount = outerLoopCount * innerLoopCount;
    VajramKryonGraph graph =
        this.graph
            //    .maxParallelismPerCore(1)
            .build();
    graph.registerInputBatchers(
        simpleInputBatcher(
            graph.getVajramIdByVajramDefType((Add.class)),
            () -> new InputBatcherImpl(innerLoopCount)));
    long javaNativeTimeNs = javaMethodBenchmark(FormulaTest::syncFormula, loopCount);
    long javaFuturesTimeNs = Util.javaFuturesBenchmark(FormulaTest::asyncFormula, loopCount);
    @SuppressWarnings("unchecked")
    CompletableFuture<Integer>[] futures = new CompletableFuture[loopCount];
    KryonExecutorMetrics[] metrics = new KryonExecutorMetrics[outerLoopCount];
    long timeToCreateExecutors = 0;
    long timeToEnqueueVajram = 0;
    long startTime = System.nanoTime();
    for (int outer_i = 0; outer_i < outerLoopCount; outer_i++) {
      long iterStartTime = System.nanoTime();
      FormulaRequestContext requestContext = new FormulaRequestContext(100, 20, 5, "formulaTest");
      try (KrystexVajramExecutor krystexVajramExecutor =
          graph.createExecutor(
              KrystexVajramExecutorConfig.builder()
                  .kryonExecutorConfigBuilder(
                      KryonExecutorConfig.builder()
                          .executorId("formulaTest")
                          .executorService(executor)
                          .kryonExecStrategy(DIRECT))
                  .build())) {
        timeToCreateExecutors += System.nanoTime() - iterStartTime;
        metrics[outer_i] =
            ((KryonExecutor) krystexVajramExecutor.getKrystalExecutor()).getKryonMetrics();
        for (int inner_i = 0; inner_i < innerLoopCount; inner_i++) {
          int iterationNum = outer_i * innerLoopCount + inner_i;
          long enqueueStart = System.nanoTime();
          futures[iterationNum] =
              executeVajram(krystexVajramExecutor, iterationNum, requestContext);
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
        .succeedsWithin(ofSeconds(1));
    assertThat(Add.CALL_COUNTER.sum()).isEqualTo(outerLoopCount);
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
      Processor: Apple M1 Pro

      Benchmark Results:
        Total java method time: 16,879,000
        Total java futures time: 77,778,625
        Outer Loop Count: 1,000
        Inner Loop Count: 1,000
        Avg. time to Create Executors:12,875 ns
        Avg. time to Enqueue vajrams:1,298 ns
        Avg. time to execute vajrams:19,205 ns
        Throughput executions/s: 52068
        CommandsQueuedCount: 1,002,000
        CommandQueueBypassedCount: 8,000
        Platform overhead over native code: 19,189 ns per request
        Platform overhead over reactive code: 19,128 ns per request
        maxActiveLeasesPerObject: 1, peakAvgActiveLeasesPerObject: 1.0, maxPoolSize: 1
    */
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
      KrystexVajramExecutor krystexVajramExecutor, int value, FormulaRequestContext rc) {
    return krystexVajramExecutor.execute(
        Formula_ReqImmutPojo._builder().a(rc.a + value).p(rc.p + value).q(rc.q + value)._build(),
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
        .thenApply(
            unused -> {
              int a = numerator.getNow(null);
              int b = sum.getNow(null);
              return a / b;
            });
  }

  private record FormulaRequestContext(int a, int p, int q, String requestId) {}

  @Test
  void formula_success_withAllMockedDependencies() {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputBatchers(
        simpleInputBatcher(
            graph.getVajramIdByVajramDefType(Add.class), () -> new InputBatcherImpl(100)));
    KrystexVajramExecutorConfig executorConfigBuilder =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId(REQUEST_ID)
                    .executorService(executorLease.get()))
            .build();
    FormulaRequestContext requestContext = new FormulaRequestContext(100, 20, 5, REQUEST_ID);
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            prepareForTest(executorConfigBuilder, requestLevelCache)
                .withMock(
                    Add_FacImmutPojo._builder().numberOne(20).numberTwo(5)._build(),
                    Errable.withValue(25))
                .withMock(
                    Divide_FacImmutPojo._builder().numerator(100).denominator(25)._build(),
                    Errable.withValue(4))
                .buildConfig())) {
      future = executeVajram(krystexVajramExecutor, 0, requestContext);
    }
    assertThat(future).succeedsWithin(TEST_TIMEOUT).isEqualTo(4);
    assertThat(Add.CALL_COUNTER.sum()).isEqualTo(0);
  }

  @Test
  void formula_success_with_mockedDependencyAdder() {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputBatchers(
        simpleInputBatcher(
            graph.getVajramIdByVajramDefType(Add.class), () -> new InputBatcherImpl(100)));
    KrystexVajramExecutorConfig kryonExecutorConfigBuilder =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId(REQUEST_ID)
                    .executorService(executorLease.get())
                    .kryonExecStrategy(BATCH)
                    .graphTraversalStrategy(GraphTraversalStrategy.DEPTH))
            .build();
    FormulaRequestContext requestContext = new FormulaRequestContext(100, 20, 5, REQUEST_ID);
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            prepareForTest(kryonExecutorConfigBuilder, requestLevelCache)
                .withMock(
                    Add_FacImmutPojo._builder().numberOne(20).numberTwo(5)._build(),
                    Errable.withValue(25))
                .buildConfig())) {
      future = executeVajram(krystexVajramExecutor, 0, requestContext);
    }
    assertThat(future).succeedsWithin(TEST_TIMEOUT).isEqualTo(4);
    assertThat(Add.CALL_COUNTER.sum()).isEqualTo(0);
  }

  @Test
  void formula_success_with_mockedDependencyDivider() {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputBatchers(
        simpleInputBatcher(
            graph.getVajramIdByVajramDefType(Add.class), () -> new InputBatcherImpl(100)));
    KrystexVajramExecutorConfig executorConfig =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorId(REQUEST_ID)
                    .executorService(executorLease.get())
                    .kryonExecStrategy(BATCH)
                    .graphTraversalStrategy(GraphTraversalStrategy.DEPTH))
            .build();
    FormulaRequestContext requestContext = new FormulaRequestContext(100, 20, 5, REQUEST_ID);
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            prepareForTest(executorConfig, requestLevelCache)
                .withMock(
                    Divide_FacImmutPojo._builder().numerator(100).denominator(25)._build(),
                    Errable.withValue(4))
                .buildConfig())) {
      future = executeVajram(krystexVajramExecutor, 0, requestContext);
    }
    assertThat(future).succeedsWithin(TEST_TIMEOUT).isEqualTo(4);
    assertThat(Add.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @Test
  void formula_failure() {
    CompletableFuture<Integer> future;
    VajramKryonGraph graph = this.graph.build();
    graph.registerInputBatchers(
        simpleInputBatcher(
            graph.getVajramIdByVajramDefType(Add.class), () -> new InputBatcherImpl(100)));
    KrystexVajramExecutorConfig executorConfig =
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .executorService(executorLease.get())
                    .kryonExecStrategy(BATCH)
                    .graphTraversalStrategy(GraphTraversalStrategy.DEPTH))
            .build();
    FormulaRequestContext requestContext = new FormulaRequestContext(100, 0, 0, REQUEST_ID);
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            prepareForTest(executorConfig, requestLevelCache)
                .withMock(
                    Add_FacImmutPojo._builder().numberOne(0).numberTwo(0)._build(),
                    Errable.withValue(0))
                .buildConfig())) {
      future = executeVajram(krystexVajramExecutor, 0, requestContext);
    }
    assertThat(future)
        .failsWithin(TEST_TIMEOUT)
        .withThrowableOfType(ExecutionException.class)
        .withCauseInstanceOf(ArithmeticException.class)
        .withMessage("java.lang.ArithmeticException: / by zero");
  }

  private SingleThreadExecutor[] getExecutors(int count) {
    SingleThreadExecutor[] singleThreadedExecutors = new SingleThreadExecutor[count];
    for (int i = 0; i < count; i++) {
      singleThreadedExecutors[i] = EXECUTOR_LEASES[i].get();
    }

    return singleThreadedExecutors;
  }

  private static @NonNull VajramGuiceInputInjector injectAdderFailure() {
    return new VajramGuiceInputInjector(
        createInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(Boolean.class).annotatedWith(named(FAIL_ADDER_FLAG)).toInstance(true);
              }
            }));
  }

  public static InputBatcherConfig simpleInputBatcher(
      VajramID vajramID, Supplier<InputBatcher> inputBatcherSupplier) {
    return new InputBatcherConfig(
        ImmutableMap.of(vajramID, ImmutableList.of(simple(inputBatcherSupplier))));
  }
}

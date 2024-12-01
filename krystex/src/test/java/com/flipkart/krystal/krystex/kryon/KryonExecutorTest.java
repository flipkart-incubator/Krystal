package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.annos.ExternalInvocation.ExternalInvocations.externalInvocation;
import static com.flipkart.krystal.data.Errable.computeErrableFrom;
import static com.flipkart.krystal.data.Errable.withValue;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.BREADTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.GRANULAR;
import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Collections.emptySet;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.caching.RequestLevelCache;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KryonExecutorTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(1);
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("KryonExecutorTest", 4);
  }

  private Lease<SingleThreadExecutor> executorLease;
  private KryonExecutor kryonExecutor;
  private KryonDefinitionRegistry kryonDefinitionRegistry;
  private LogicDefinitionRegistry logicDefinitionRegistry;
  private RequestLevelCache requestLevelCache;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    this.requestLevelCache = new RequestLevelCache();
    this.logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
  }

  @AfterEach
  void tearDown() {
    Optional.ofNullable(kryonExecutor).ifPresent(KryonExecutor::close);
    executorLease.close();
  }

  /** Executing same kryon multiple times in a single execution */
  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void multiRequestExecution(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy)
      throws Exception {
    // This is redundant. This should Ideally move to a paramterized @BeforeEach method or after
    // parametrizing this at the test class level.
    // This is currently not supported in jupiter-junit:5.9.x.
    // It is planned to be supported in jupiter-junit:5.10
    // (Ref: https://github.com/junit-team/junit5/issues/878)
    // Move this to the @BeforeEach method after 5.10 is released.
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    KryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newKryonDefinition(
            "kryon",
            emptySet(),
            newComputeLogic("kryonLogic", emptySet(), dependencyValues -> "computed_value")
                .kryonLogicId(),
            ImmutableMap.of(),
            ImmutableList.of(),
            null,
            ElementTags.of(externalInvocation(true)));
    CompletableFuture<Object> future1 =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            Facets.empty(),
            KryonExecutionConfig.builder().executionId("req_1").build());
    CompletableFuture<Object> future2 =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            Facets.empty(),
            KryonExecutionConfig.builder().executionId("req_2").build());

    kryonExecutor.close();
    assertEquals("computed_value", timedGet(future1));
    assertEquals("computed_value", timedGet(future2));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void multiRequestExecutionWithNullRequestId(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    KryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newKryonDefinition(
            "kryon",
            emptySet(),
            newComputeLogic("kryonLogic", emptySet(), dependencyValues -> "computed_value")
                .kryonLogicId(),
            ImmutableMap.of(),
            ImmutableList.of(),
            null,
            ElementTags.of(List.of(externalInvocation(true))));

    CompletableFuture<Object> future1 =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            Facets.empty(),
            KryonExecutionConfig.builder().executionId("req_1").build());
    assertThatThrownBy(
            () -> kryonExecutor.executeKryon(kryonDefinition.kryonId(), Facets.empty(), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("executionConfig can not be null");
    kryonExecutor.close();
    assertThat(future1).succeedsWithin(1, SECONDS).isEqualTo("computed_value");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_noDependencies_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy)
      throws Exception {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    KryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newKryonDefinition(
            "kryon",
            emptySet(),
            newComputeLogic("kryonLogic", emptySet(), dependencyValues -> "computed_value")
                .kryonLogicId(),
            ImmutableMap.of(),
            ImmutableList.of(),
            null,
            ElementTags.of(externalInvocation(true)));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            Facets.empty(),
            KryonExecutionConfig.builder().executionId("req_1").build());
    kryonExecutor.close();
    assertEquals("computed_value", timedGet(future));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_unboundInputs_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy)
      throws Exception {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    String logicId = "requestExecution_noDependencies_success_nodeName";
    KryonId kryonId =
        kryonDefinitionRegistry
            .newKryonDefinition(
                logicId,
                Set.of("a", "b", "c"),
                newComputeLogic(
                        logicId,
                        Set.of("a", "b", "c"),
                        inputs ->
                            "computed_values: a=%s;b=%s;c=%s"
                                .formatted(
                                    inputs.getInputValue("a").value().orElseThrow(),
                                    inputs.getInputValue("b").value().orElseThrow(),
                                    inputs.getInputValue("c").value().orElseThrow()))
                    .kryonLogicId(),
                ImmutableMap.of(),
                ImmutableList.of(),
                null,
                ElementTags.of(externalInvocation(true)))
            .kryonId();
    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            kryonId,
            new Facets(ImmutableMap.of("a", withValue(1), "b", withValue(2), "c", withValue("3"))),
            KryonExecutionConfig.builder().executionId("r").build());
    kryonExecutor.close();
    assertEquals("computed_values: a=1;b=2;c=3", timedGet(future));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_singleDependency_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy)
      throws Exception {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    KryonDefinition n1 =
        kryonDefinitionRegistry.newKryonDefinition(
            "n1",
            emptySet(),
            newComputeLogic("n1_logic", emptySet(), dependencyValues -> "dependency_value")
                .kryonLogicId());

    KryonDefinition n2 =
        kryonDefinitionRegistry.newKryonDefinition(
            "n2",
            emptySet(),
            newComputeLogic(
                    "n2_logic",
                    Set.of("dep"),
                    dependencyValues ->
                        dependencyValues
                                .getDepValue("dep")
                                .values()
                                .values()
                                .iterator()
                                .next()
                                .value()
                                .orElseThrow()
                            + ":computed_value")
                .kryonLogicId(),
            ImmutableMap.of("dep", n1.kryonId()),
            ImmutableList.of(),
            null,
            ElementTags.of(externalInvocation(true)));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            n2.kryonId(), Facets.empty(), KryonExecutionConfig.builder().executionId("r1").build());
    kryonExecutor.close();
    assertEquals("dependency_value:computed_value", timedGet(future));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_multiLevelDependencies_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    String l1Dep = "requestExecution_multiLevelDependencies_level1";
    kryonDefinitionRegistry.newKryonDefinition(
        l1Dep,
        emptySet(),
        newComputeLogic(l1Dep, emptySet(), dependencyValues -> "l1").kryonLogicId());

    String l2Dep = "requestExecution_multiLevelDependencies_level2";
    kryonDefinitionRegistry.newKryonDefinition(
        l2Dep,
        emptySet(),
        newComputeLogic(
                l2Dep,
                Set.of("dep"),
                dependencyValues ->
                    dependencyValues
                            .getDepValue("dep")
                            .values()
                            .values()
                            .iterator()
                            .next()
                            .value()
                            .orElseThrow()
                        + ":l2")
            .kryonLogicId(),
        ImmutableMap.of("dep", new KryonId(l1Dep)));

    String l3Dep = "requestExecution_multiLevelDependencies_level3";
    kryonDefinitionRegistry.newKryonDefinition(
        l3Dep,
        emptySet(),
        newComputeLogic(
                l3Dep,
                Set.of("dep"),
                dependencyValues -> {
                  return dependencyValues
                          .getDepValue("dep")
                          .values()
                          .values()
                          .iterator()
                          .next()
                          .value()
                          .orElseThrow()
                      + ":l3";
                })
            .kryonLogicId(),
        ImmutableMap.of("dep", new KryonId(l2Dep)));

    String l4Dep = "requestExecution_multiLevelDependencies_level4";
    kryonDefinitionRegistry.newKryonDefinition(
        l4Dep,
        emptySet(),
        newComputeLogic(
                l4Dep,
                Set.of("dep"),
                dependencyValues ->
                    dependencyValues
                            .getDepValue("dep")
                            .values()
                            .values()
                            .iterator()
                            .next()
                            .value()
                            .orElseThrow()
                        + ":l4")
            .kryonLogicId(),
        ImmutableMap.of("dep", new KryonId(l3Dep)),
        ImmutableList.of(),
        null,
        ElementTags.of(List.of(externalInvocation(true))));

    Facets facets = Facets.empty();
    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            kryonDefinitionRegistry
                .newKryonDefinition(
                    "requestExecution_multiLevelDependencies_final",
                    emptySet(),
                    newComputeLogic(
                            "requestExecution_multiLevelDependencies_final",
                            Set.of("dep"),
                            dependencyValues ->
                                dependencyValues
                                        .getDepValue("dep")
                                        .values()
                                        .values()
                                        .iterator()
                                        .next()
                                        .value()
                                        .orElseThrow()
                                    + ":final")
                        .kryonLogicId(),
                    ImmutableMap.of("dep", new KryonId(l4Dep)),
                    ImmutableList.of(),
                    null,
                    ElementTags.of(externalInvocation(true)))
                .kryonId(),
            facets,
            KryonExecutionConfig.builder().executionId("r").build());
    kryonExecutor.close();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("l1:l2:l3:l4:final");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    String dep1 =
        "executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce_dep1";
    kryonDefinitionRegistry.newKryonDefinition(
        dep1,
        emptySet(),
        newComputeLogic(dep1, emptySet(), dependencyValues -> "l1").kryonLogicId());

    String dep2 =
        "executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce_dep2";
    kryonDefinitionRegistry.newKryonDefinition(
        dep2,
        emptySet(),
        newComputeLogic(dep2, emptySet(), dependencyValues -> "l2").kryonLogicId());

    LongAdder numberOfExecutions = new LongAdder();
    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            kryonDefinitionRegistry
                .newKryonDefinition(
                    "executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce_final",
                    emptySet(),
                    newComputeLogic(
                            "executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce_final",
                            Set.of("dep1", "dep2"),
                            dependencyValues -> {
                              numberOfExecutions.increment();
                              return dependencyValues
                                      .getDepValue("dep1")
                                      .values()
                                      .values()
                                      .iterator()
                                      .next()
                                      .value()
                                      .orElseThrow()
                                  + ":"
                                  + dependencyValues
                                      .getDepValue("dep2")
                                      .values()
                                      .values()
                                      .iterator()
                                      .next()
                                      .value()
                                      .orElseThrow()
                                  + ":final";
                            })
                        .kryonLogicId(),
                    ImmutableMap.of("dep1", new KryonId(dep1), "dep2", new KryonId(dep2)),
                    ImmutableList.of(),
                    null,
                    ElementTags.of(externalInvocation(true)))
                .kryonId(),
            Facets.empty(),
            KryonExecutionConfig.builder().executionId("r").build());
    kryonExecutor.close();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("l1:l2:final");
    assertThat(numberOfExecutions.sum()).isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void close_preventsNewExecutionRequests(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    kryonExecutor.close();
    assertThrows(
        Exception.class,
        () ->
            kryonExecutor.executeKryon(
                kryonDefinitionRegistry
                    .newKryonDefinition(
                        "shutdown_preventsNewExecutionRequests",
                        ImmutableSet.of(),
                        newComputeLogic(
                                "shutdown_preventsNewExecutionRequests",
                                ImmutableSet.of(),
                                dependencyValues -> "")
                            .kryonLogicId())
                    .kryonId(),
                Facets.empty(),
                KryonExecutionConfig.builder().executionId("req_1").build()));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void shutdownNow_terminatesPendingWork(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    KryonDefinition n1 =
        kryonDefinitionRegistry.newKryonDefinition(
            "n1",
            emptySet(),
            newComputeLogic("n1_logic", emptySet(), dependencyValues -> "dependency_value")
                .kryonLogicId());

    CountDownLatch countDownLatch = new CountDownLatch(1);
    KryonDefinition n2 =
        kryonDefinitionRegistry.newKryonDefinition(
            "n2",
            emptySet(),
            newIoLogic(
                    "n2_logic",
                    Set.of("dep"),
                    dependencyValues -> {
                      return runAsync(
                              () -> {
                                while (countDownLatch.getCount() > 0) {
                                  try {
                                    countDownLatch.await();
                                  } catch (Exception ignored) {
                                  }
                                }
                              },
                              newSingleThreadExecutor())
                          .handle(
                              (unused, throwable) -> {
                                return dependencyValues
                                        .getDepValue("dep")
                                        .values()
                                        .values()
                                        .iterator()
                                        .next()
                                        .value()
                                        .orElseThrow()
                                    + ":computed_value";
                              });
                    })
                .kryonLogicId(),
            ImmutableMap.of("dep", n1.kryonId()),
            ImmutableList.of(),
            null,
            ElementTags.of(List.of(externalInvocation(true))));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            n2.kryonId(), Facets.empty(), KryonExecutionConfig.builder().executionId("r1").build());
    kryonExecutor.close();
    kryonExecutor.shutdownNow();
    countDownLatch.countDown();
    assertThat(future)
        .failsWithin(1, SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(RejectedExecutionException.class)
        .withMessage(
            "java.util.concurrent.RejectedExecutionException: Kryon Executor shutdown requested.");
  }

  /* So that bad testcases do not hang indefinitely.*/
  private static <T> T timedGet(CompletableFuture<T> future)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(TIMEOUT.getSeconds(), SECONDS);
  }

  private <T> OutputLogicDefinition<T> newComputeLogic(
      String kryonId, Set<String> inputs, Function<Facets, T> logic) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(
            new KryonLogicId(new KryonId(kryonId), kryonId),
            inputs,
            inputsList ->
                inputsList.stream()
                    .collect(toImmutableMap(identity(), computeErrableFrom(logic)))
                    .entrySet()
                    .stream()
                    .collect(toImmutableMap(Entry::getKey, e -> e.getValue().toFuture())),
            emptyTags());

    logicDefinitionRegistry.addOutputLogic(def);
    return def;
  }

  private <T> OutputLogicDefinition<T> newIoLogic(
      String kryonId, Set<String> inputs, Function<Facets, CompletableFuture<T>> logic) {
    IOLogicDefinition<T> def =
        new IOLogicDefinition<>(
            new KryonLogicId(new KryonId(kryonId), kryonId),
            inputs,
            inputsList ->
                inputsList.stream().collect(toImmutableMap(identity(), logic)).entrySet().stream()
                    .collect(toImmutableMap(Entry::getKey, Entry::getValue)),
            emptyTags());

    logicDefinitionRegistry.addOutputLogic(def);
    return def;
  }

  private KryonExecutor getKryonExecutor(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    var config =
        KryonExecutorConfig.builder()
            .singleThreadExecutor(executorLease.get())
            .kryonExecStrategy(kryonExecStrategy)
            .graphTraversalStrategy(graphTraversalStrategy)
            .requestScopedKryonDecoratorConfig(
                RequestLevelCache.DECORATOR_TYPE,
                new KryonDecoratorConfig(
                    RequestLevelCache.DECORATOR_TYPE,
                    _c -> true,
                    _c -> RequestLevelCache.DECORATOR_TYPE,
                    _c -> requestLevelCache))
            .build();
    return new KryonExecutor(kryonDefinitionRegistry, config, "test");
  }

  public static Stream<Arguments> executorConfigsToTest() {
    return Stream.of(
        Arguments.of(BATCH, DEPTH),
        Arguments.of(BATCH, BREADTH),
        Arguments.of(GRANULAR, DEPTH),
        Arguments.of(GRANULAR, BREADTH));
  }
}

package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.data.Errable.withValue;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.BREADTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.GRANULAR;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Collections.emptySet;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.ForkJoinExecutorPool;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KryonExecutorTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(1);

  private KryonExecutor kryonExecutor;
  private KryonDefinitionRegistry kryonDefinitionRegistry;
  private LogicDefinitionRegistry logicDefinitionRegistry;

  @BeforeEach
  void setUp() {
    this.logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
  }

  @AfterEach
  void tearDown() {
    Optional.ofNullable(kryonExecutor).ifPresent(KryonExecutor::close);
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
                .kryonLogicId());

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
                .kryonLogicId());

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
                .kryonLogicId());

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
                    .kryonLogicId())
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
            ImmutableMap.of("dep", n1.kryonId()));

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
        ImmutableMap.of("dep", new KryonId(l3Dep)));

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
                    ImmutableMap.of("dep", new KryonId(l4Dep)))
                .kryonId(),
            facets,
            KryonExecutionConfig.builder().executionId("r").build());
    kryonExecutor.close();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("l1:l2:l3:l4:final");
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
                    .collect(toImmutableMap(identity(), Errable.computeErrableFrom(logic)))
                    .entrySet()
                    .stream()
                    .collect(toImmutableMap(Entry::getKey, e -> e.getValue().toFuture())),
            ImmutableMap.of());

    logicDefinitionRegistry.addOutputLogic(def);
    return def;
  }

  private KryonExecutor getKryonExecutor(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    return new KryonExecutor(
        kryonDefinitionRegistry,
        new ForkJoinExecutorPool(1),
        KryonExecutorConfig.builder()
            .kryonExecStrategy(kryonExecStrategy)
            .graphTraversalStrategy(graphTraversalStrategy)
            .build(),
        "test");
  }

  public static Stream<Arguments> executorConfigsToTest() {
    return Stream.of(
        Arguments.of(BATCH, DEPTH),
        Arguments.of(BATCH, BREADTH),
        Arguments.of(GRANULAR, DEPTH),
        Arguments.of(GRANULAR, BREADTH));
  }
}

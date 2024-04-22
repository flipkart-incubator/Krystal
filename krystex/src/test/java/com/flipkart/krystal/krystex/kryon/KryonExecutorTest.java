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
import com.flipkart.krystal.data.FacetsMapBuilder;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.data.Response;
import com.flipkart.krystal.data.SimpleRequest;
import com.flipkart.krystal.data.SimpleRequestBuilder;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.ForkJoinExecutorPool;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.FacetsFromRequest;
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.units.qual.K;
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
    String kryonName = "kryon";
    KryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newKryonDefinition(
            kryonName,
            emptySet(),
            newComputeLogic(kryonName, emptySet(), dependencyValues -> "computed_value")
                .kryonLogicId(),
            newCreateNewRequestLogic(kryonName),
            newFacetsFromRequestLogic(kryonName));

    CompletableFuture<Object> future1 =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            SimpleRequest.empty(),
            KryonExecutionConfig.builder().executionId("req_1").build());
    CompletableFuture<Object> future2 =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            SimpleRequest.empty(),
            KryonExecutionConfig.builder().executionId("req_2").build());

    kryonExecutor.flush();
    assertEquals("computed_value", timedGet(future1));
    assertEquals("computed_value", timedGet(future2));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void multiRequestExecutionWithNullRequestId(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    String kryonName = "kryon";
    KryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newKryonDefinition(
            kryonName,
            emptySet(),
            newComputeLogic(kryonName, emptySet(), dependencyValues -> "computed_value")
                .kryonLogicId(),
            newCreateNewRequestLogic(kryonName),
            newFacetsFromRequestLogic(kryonName));

    CompletableFuture<Object> future1 =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            SimpleRequest.empty(),
            KryonExecutionConfig.builder().executionId("req_1").build());
    assertThatThrownBy(
            () ->
                kryonExecutor.executeKryon(kryonDefinition.kryonId(), SimpleRequest.empty(), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("executionConfig can not be null");
    kryonExecutor.flush();
    assertThat(future1).succeedsWithin(1, SECONDS).isEqualTo("computed_value");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_noDependencies_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy)
      throws Exception {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    String kryonName = "kryon";
    KryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newKryonDefinition(
            kryonName,
            emptySet(),
            newComputeLogic(kryonName, emptySet(), dependencyValues -> "computed_value")
                .kryonLogicId(),
            newCreateNewRequestLogic(kryonName),
            newFacetsFromRequestLogic(kryonName));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            SimpleRequest.empty(),
            KryonExecutionConfig.builder().executionId("req_1").build());
    kryonExecutor.flush();
    assertEquals("computed_value", timedGet(future));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_unboundInputs_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy)
      throws Exception {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    String kryonName = "requestExecution_noDependencies_success_nodeName";
    KryonId kryonId =
        kryonDefinitionRegistry
            .newKryonDefinition(
                kryonName,
                Set.of(1, 2, 3),
                newComputeLogic(
                        kryonName,
                        Set.of(1, 2, 3),
                        inputs ->
                            "computed_values: a=%s;b=%s;c=%s"
                                .formatted(
                                    inputs._get(1).valueOrThrow(),
                                    inputs._get(2).valueOrThrow(),
                                    inputs._get(3).valueOrThrow()))
                    .kryonLogicId(),
                newCreateNewRequestLogic(kryonName),
                newFacetsFromRequestLogic(kryonName))
            .kryonId();
    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            kryonId,
            new SimpleRequestBuilder<>(
                ImmutableMap.of(1, withValue(1), 2, withValue(2), 3, withValue("3"))),
            KryonExecutionConfig.builder().executionId("r").build());
    kryonExecutor.flush();
    assertEquals("computed_values: a=1;b=2;c=3", timedGet(future));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_singleDependency_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy)
      throws Exception {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    String kryonName = "n1";
    KryonDefinition n1 =
        kryonDefinitionRegistry.newKryonDefinition(
            kryonName,
            emptySet(),
            newComputeLogic(kryonName, emptySet(), dependencyValues -> "dependency_value")
                .kryonLogicId(),
            newCreateNewRequestLogic(kryonName),
            newFacetsFromRequestLogic(kryonName));

    KryonDefinition n2 =
        kryonDefinitionRegistry.newKryonDefinition(
            "n2",
            emptySet(),
            newComputeLogic(
                    "n2",
                    Set.of(1),
                    dependencyValues ->
                        dependencyValues
                                ._getDepResponse(1)
                                .responses()
                                .iterator()
                                .next()
                                .response()
                                .valueOrThrow()
                            + ":computed_value")
                .kryonLogicId(),
            ImmutableMap.of(1, n1.kryonId()),
            newCreateNewRequestLogic("n2"),
            newFacetsFromRequestLogic("n2"));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            n2.kryonId(),
            n2.createNewRequest().logic().newRequestBuilder(),
            KryonExecutionConfig.builder().executionId("r1").build());
    kryonExecutor.flush();
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
        newComputeLogic(l1Dep, emptySet(), dependencyValues1 -> "l1").kryonLogicId(),
        newCreateNewRequestLogic(l1Dep),
        newFacetsFromRequestLogic(l1Dep));

    String l2Dep = "requestExecution_multiLevelDependencies_level2";
    kryonDefinitionRegistry.newKryonDefinition(
        l2Dep,
        emptySet(),
        newComputeLogic(
                l2Dep,
                Set.of(1),
                dependencyValues ->
                    dependencyValues._getDepResponse(1).responses().stream()
                            .map(Response::response)
                            .iterator()
                            .next()
                            .valueOrThrow()
                        + ":l2")
            .kryonLogicId(),
        ImmutableMap.of(1, new KryonId(l1Dep)),
        newCreateNewRequestLogic(l2Dep),
        newFacetsFromRequestLogic(l2Dep));

    String l3Dep = "requestExecution_multiLevelDependencies_level3";
    kryonDefinitionRegistry.newKryonDefinition(
        l3Dep,
        emptySet(),
        newComputeLogic(
                l3Dep,
                Set.of(1),
                dependencyValues -> {
                  return dependencyValues._getDepResponse(1).responses().stream()
                          .map(Response::response)
                          .iterator()
                          .next()
                          .valueOrThrow()
                      + ":l3";
                })
            .kryonLogicId(),
        ImmutableMap.of(1, new KryonId(l2Dep)),
        newCreateNewRequestLogic(l3Dep),
        newFacetsFromRequestLogic(l3Dep));

    String l4Dep = "requestExecution_multiLevelDependencies_level4";
    kryonDefinitionRegistry.newKryonDefinition(
        l4Dep,
        emptySet(),
        newComputeLogic(
                l4Dep,
                Set.of(1),
                dependencyValues ->
                    dependencyValues._getDepResponse(1).responses().stream()
                            .map(Response::response)
                            .iterator()
                            .next()
                            .valueOrThrow()
                        + ":l4")
            .kryonLogicId(),
        ImmutableMap.of(1, new KryonId(l3Dep)),
        newCreateNewRequestLogic(l4Dep),
        newFacetsFromRequestLogic(l4Dep));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            kryonDefinitionRegistry
                .newKryonDefinition(
                    "requestExecution_multiLevelDependencies_final",
                    emptySet(),
                    newComputeLogic(
                            "requestExecution_multiLevelDependencies_final",
                            Set.of(1),
                            dependencyValues ->
                                dependencyValues._getDepResponse(1).responses().stream()
                                        .map(Response::response)
                                        .iterator()
                                        .next()
                                        .valueOrThrow()
                                    + ":final")
                        .kryonLogicId(),
                    ImmutableMap.of(1, new KryonId(l4Dep)),
                    newCreateNewRequestLogic(l4Dep),
                    newFacetsFromRequestLogic(l4Dep))
                .kryonId(),
            SimpleRequest.empty(),
            KryonExecutionConfig.builder().executionId("r").build());
    kryonExecutor.flush();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("l1:l2:l3:l4:final");
  }

  @NonNull
  private static LogicDefinition<FacetsFromRequest> newFacetsFromRequestLogic(String l1Dep) {
    return new LogicDefinition<>(
        new KryonLogicId(new KryonId(l1Dep), l1Dep + ":facetsFromRequest"), FacetsMapBuilder::new);
  }

  @NonNull
  private static LogicDefinition<CreateNewRequest> newCreateNewRequestLogic(String kryonName) {
    return new LogicDefinition<>(
        new KryonLogicId(new KryonId(kryonName), kryonName + ":newRequest"),
        SimpleRequestBuilder::new);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void close_preventsNewExecutionRequests(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    kryonExecutor.close();
    String kryonName = "shutdown_preventsNewExecutionRequests";
    assertThrows(
        Exception.class,
        () ->
            kryonExecutor.executeKryon(
                kryonDefinitionRegistry
                    .newKryonDefinition(
                        kryonName,
                        ImmutableSet.of(),
                        newComputeLogic(kryonName, ImmutableSet.of(), dependencyValues -> "")
                            .kryonLogicId(),
                        newCreateNewRequestLogic(kryonName),
                        newFacetsFromRequestLogic(kryonName))
                    .kryonId(),
                SimpleRequest.empty(),
                KryonExecutionConfig.builder().executionId("req_1").build()));
  }

  /* So that bad testcases do not hang indefinitely.*/
  private static <T> T timedGet(CompletableFuture<T> future)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(TIMEOUT.getSeconds(), SECONDS);
  }

  private <T> OutputLogicDefinition<T> newComputeLogic(
      String kryonId, Set<Integer> inputs, Function<Facets, T> logic) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(
            new KryonLogicId(new KryonId(kryonId), kryonId),
            inputs,
            inputsList ->
                inputsList.stream()
                    .collect(toImmutableMap(identity(), Errable.errableFrom(logic)))
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

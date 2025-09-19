package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.data.Errable.computeErrableFrom;
import static com.flipkart.krystal.data.Errable.withValue;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.BREADTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;
import static com.flipkart.krystal.krystex.testutils.SimpleFacet.dependency;
import static com.flipkart.krystal.krystex.testutils.SimpleFacet.input;
import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Collections.emptySet;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.annos.InvocableOutsideGraph.Creator;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.core.GraphExecutionData;
import com.flipkart.krystal.core.OutputLogicExecutionResults;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.GraphExecutionLogic;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.caching.RequestLevelCache;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.FacetsFromRequest;
import com.flipkart.krystal.krystex.testutils.FacetValuesMap;
import com.flipkart.krystal.krystex.testutils.FacetValuesMapBuilder;
import com.flipkart.krystal.krystex.testutils.SimpleDep;
import com.flipkart.krystal.krystex.testutils.SimpleFacet;
import com.flipkart.krystal.krystex.testutils.SimpleImmutRequest;
import com.flipkart.krystal.krystex.testutils.SimpleRequest;
import com.flipkart.krystal.krystex.testutils.SimpleRequestBuilder;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("unchecked")
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
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    // This is redundant. This should Ideally move to a paramterized @BeforeEach method or after
    // parametrizing this at the test class level.
    // This is currently not supported in jupiter-junit:5.9.x.
    // It is planned to be supported in jupiter-junit:5.10
    // (Ref: https://github.com/junit-team/junit5/issues/878)
    // Move this to the @BeforeEach method after 5.10 is released.
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    VajramID kryonName = vajramID("kryon");
    VajramKryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            kryonName,
            emptySet(),
            newComputeLogic(kryonName, emptySet(), dependencyValues -> "computed_value")
                .kryonLogicId(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(kryonName, emptySet()),
            newFacetsFromRequestLogic(kryonName, emptySet()),
            _graphExecData ->
                _graphExecData
                    .executionItems()
                    .forEach(e -> _graphExecData.communicationFacade().executeOutputLogic(e)),
            ElementTags.of(InvocableOutsideGraph.Creator.create()));

    CompletableFuture<Object> future1 =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(kryonDefinition.vajramID()),
            KryonExecutionConfig.builder().executionId("req_1").build());
    CompletableFuture<Object> future2 =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(kryonDefinition.vajramID()),
            KryonExecutionConfig.builder().executionId("req_2").build());

    kryonExecutor.close();
    assertThat(future1).succeedsWithin(TIMEOUT).isEqualTo("computed_value");
    assertThat(future2).succeedsWithin(TIMEOUT).isEqualTo("computed_value");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void multiRequestExecutionWithNullRequestId(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    VajramID kryonName = vajramID("kryon");
    VajramKryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            kryonName,
            emptySet(),
            newComputeLogic(kryonName, emptySet(), dependencyValues -> "computed_value")
                .kryonLogicId(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(kryonName, emptySet()),
            newFacetsFromRequestLogic(kryonName, emptySet()),
            _graphExecData ->
                _graphExecData
                    .executionItems()
                    .forEach(e -> _graphExecData.communicationFacade().executeOutputLogic(e)),
            ElementTags.of(List.of(InvocableOutsideGraph.Creator.create())));

    CompletableFuture<Object> future1 =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(kryonDefinition.vajramID()),
            KryonExecutionConfig.builder().executionId("req_1").build());
    assertThatThrownBy(
            () ->
                kryonExecutor.executeKryon(
                    SimpleImmutRequest.empty(kryonDefinition.vajramID()), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("executionConfig can not be null");
    kryonExecutor.close();
    assertThat(future1).succeedsWithin(1, SECONDS).isEqualTo("computed_value");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_noDependencies_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    VajramID kryonName = vajramID("kryon");
    VajramKryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            kryonName,
            emptySet(),
            newComputeLogic(kryonName, emptySet(), dependencyValues -> "computed_value")
                .kryonLogicId(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(kryonName, emptySet()),
            newFacetsFromRequestLogic(kryonName, emptySet()),
            _graphExecData ->
                _graphExecData
                    .executionItems()
                    .forEach(e -> _graphExecData.communicationFacade().executeOutputLogic(e)),
            ElementTags.of(InvocableOutsideGraph.Creator.create()));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(kryonDefinition.vajramID()),
            KryonExecutionConfig.builder().executionId("req_1").build());
    kryonExecutor.close();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("computed_value");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_unboundInputs_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    VajramID kryonName = vajramID("requestExecution_noDependencies_success_nodeName");
    ImmutableSet<SimpleFacet> inputDefs = ImmutableSet.of(input(1), input(2), input(3));
    VajramID vajramID =
        kryonDefinitionRegistry
            .newVajramKryonDefinition(
                kryonName,
                inputDefs,
                newComputeLogic(
                        kryonName,
                        Set.of(input(1), input(2), input(3)),
                        facets ->
                            "computed_values: a=%s;b=%s;c=%s"
                                .formatted(
                                    ((FacetValuesMap) facets)._getErrable(1).valueOrThrow(),
                                    ((FacetValuesMap) facets)._getErrable(2).valueOrThrow(),
                                    ((FacetValuesMap) facets)._getErrable(3).valueOrThrow()))
                    .kryonLogicId(),
                ImmutableMap.of(),
                newCreateNewRequestLogic(kryonName, inputDefs),
                newFacetsFromRequestLogic(kryonName, inputDefs),
                _graphExecData ->
                    _graphExecData
                        .executionItems()
                        .forEach(e -> _graphExecData.communicationFacade().executeOutputLogic(e)),
                ElementTags.of(InvocableOutsideGraph.Creator.create()))
            .vajramID();
    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            new SimpleRequestBuilder<>(
                    inputDefs,
                    ImmutableMap.of(1, withValue(1), 2, withValue(2), 3, withValue("3")),
                    vajramID)
                ._build(),
            KryonExecutionConfig.builder().executionId("r").build());
    kryonExecutor.close();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("computed_values: a=1;b=2;c=3");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_singleDependency_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    VajramID n1ID = vajramID("n1");
    kryonDefinitionRegistry.newVajramKryonDefinition(
        n1ID,
        emptySet(),
        newComputeLogic(n1ID, emptySet(), dependencyValues -> "dependency_value").kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(n1ID, emptySet()),
        newFacetsFromRequestLogic(n1ID, emptySet()),
        _graphExecData ->
            _graphExecData
                .executionItems()
                .forEach(e -> _graphExecData.communicationFacade().executeOutputLogic(e)),
        emptyTags());

    VajramID n2ID = vajramID("n2");
    SimpleDep n1DependsOnN1 = dependency(1, n2ID, n1ID);
    Set<SimpleDep> allFacets = Set.of(n1DependsOnN1);
    VajramKryonDefinition n2 =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            n2ID,
            allFacets,
            newComputeLogic(
                    n2ID,
                    allFacets,
                    facets ->
                        ((FacetValuesMap) facets)
                                ._getDepResponses(1)
                                .requestResponsePairs()
                                .iterator()
                                .next()
                                .response()
                                .valueOrThrow()
                            + ":computed_value")
                .kryonLogicId(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(n2ID, emptySet()),
            newFacetsFromRequestLogic(n2ID, allFacets),
            _graphExecData -> {
              CompletableFuture<Object> n1Result = new CompletableFuture<>();
              _graphExecData
                  .communicationFacade()
                  .triggerDependency(
                      n1DependsOnN1,
                      List.of(
                          new RequestResponseFuture<>(
                              new SimpleRequestBuilder<>(Set.of(), n1ID), n1Result)));
              n1Result.whenComplete((o, throwable) -> {});
            },
            ElementTags.of(InvocableOutsideGraph.Creator.create()));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            n2.createNewRequest().logic().newRequestBuilder()._build(),
            KryonExecutionConfig.builder().executionId("r1").build());
    kryonExecutor.close();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("dependency_value:computed_value");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void requestExecution_multiLevelDependencies_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    VajramID l1Dep = vajramID("requestExecution_multiLevelDependencies_level1");
    kryonDefinitionRegistry.newVajramKryonDefinition(
        l1Dep,
        emptySet(),
        newComputeLogic(l1Dep, emptySet(), facets -> "l1").kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(l1Dep, emptySet()),
        newFacetsFromRequestLogic(l1Dep, emptySet()),
        _graphExecData -> {},
        emptyTags());

    VajramID l2Dep = vajramID("requestExecution_multiLevelDependencies_level2");
    Dependency l2DependsOnL1 = dependency(1, l2Dep, l1Dep);
    Set<Dependency> allFacets = Set.of(l2DependsOnL1);
    kryonDefinitionRegistry.newVajramKryonDefinition(
        l2Dep,
        allFacets,
        newComputeLogic(
                l2Dep,
                allFacets,
                facets ->
                    ((FacetValuesMap) facets)
                            ._getDepResponses(1).requestResponsePairs().stream()
                                .map(RequestResponse::response)
                                .iterator()
                                .next()
                                .valueOrThrow()
                        + ":l2")
            .kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(l2Dep, emptySet()),
        newFacetsFromRequestLogic(l2Dep, allFacets),
        _graphExecData -> {
          CompletableFuture<Object> l1Result = new CompletableFuture<>();
          _graphExecData
              .communicationFacade()
              .triggerDependency(
                  l2DependsOnL1,
                  List.of(
                      new RequestResponseFuture<>(
                          new SimpleRequestBuilder<>(Set.of(), l1Dep), l1Result)));
          l1Result.whenComplete((o, throwable) -> {});
        },
        emptyTags());

    VajramID l3Dep = vajramID("requestExecution_multiLevelDependencies_level3");
    Dependency l3DependsOnL2 = dependency(1, l3Dep, l2Dep);
    kryonDefinitionRegistry.newVajramKryonDefinition(
        l3Dep,
        allFacets,
        newComputeLogic(
                l3Dep,
                allFacets,
                facets ->
                    ((FacetValuesMap) facets)
                            ._getDepResponses(1).requestResponsePairs().stream()
                                .map(RequestResponse::response)
                                .iterator()
                                .next()
                                .valueOrThrow()
                        + ":l3")
            .kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(l3Dep, emptySet()),
        newFacetsFromRequestLogic(l3Dep, allFacets),
        _graphExecData -> {
          CompletableFuture<Object> l2Result = new CompletableFuture<>();
          _graphExecData
              .communicationFacade()
              .triggerDependency(
                  l3DependsOnL2,
                  List.of(
                      new RequestResponseFuture<>(
                          new SimpleRequestBuilder<>(Set.of(), l2Dep), l2Result)));
          l2Result.whenComplete((o, throwable) -> {});
        },
        emptyTags());

    VajramID l4Dep = vajramID("requestExecution_multiLevelDependencies_level4");
    Dependency l4DepOnL3 = dependency(1, l4Dep, l3Dep);
    kryonDefinitionRegistry.newVajramKryonDefinition(
        l4Dep,
        allFacets,
        newComputeLogic(
                l4Dep,
                allFacets,
                facets ->
                    ((FacetValuesMap) facets)
                            ._getDepResponses(1).requestResponsePairs().stream()
                                .map(RequestResponse::response)
                                .iterator()
                                .next()
                                .valueOrThrow()
                        + ":l4")
            .kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(l4Dep, emptySet()),
        newFacetsFromRequestLogic(l4Dep, allFacets),
        _graphExecData -> {
          CompletableFuture<Object> l3Result = new CompletableFuture<>();
          _graphExecData
              .communicationFacade()
              .triggerDependency(
                  l4DepOnL3,
                  List.of(
                      new RequestResponseFuture<>(
                          new SimpleRequestBuilder<>(Set.of(), l3Dep), l3Result)));
          l3Result.whenComplete((o, throwable) -> {});
        },
        ElementTags.of(List.of(InvocableOutsideGraph.Creator.create())));

    VajramID finalKryon = vajramID("requestExecution_multiLevelDependencies_final");
    Dependency finalDepOnL4 = dependency(1, finalKryon, l4Dep);
    VajramID vajramID =
        kryonDefinitionRegistry
            .newVajramKryonDefinition(
                finalKryon,
                allFacets,
                newComputeLogic(
                        finalKryon,
                        allFacets,
                        facets ->
                            ((FacetValuesMap) facets)
                                    ._getDepResponses(1).requestResponsePairs().stream()
                                        .map(RequestResponse::response)
                                        .iterator()
                                        .next()
                                        .valueOrThrow()
                                + ":final")
                    .kryonLogicId(),
                ImmutableMap.of(),
                newCreateNewRequestLogic(finalKryon, emptySet()),
                newFacetsFromRequestLogic(finalKryon, allFacets),
                _graphExecData -> {
                  CompletableFuture<Object> l4Result = new CompletableFuture<>();
                  _graphExecData
                      .communicationFacade()
                      .triggerDependency(
                          finalDepOnL4,
                          List.of(
                              new RequestResponseFuture<>(
                                  new SimpleRequestBuilder<>(Set.of(), l4Dep), l4Result)));
                  l4Result.whenComplete((o, throwable) -> {});
                },
                ElementTags.of(Creator.create()))
            .vajramID();
    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(vajramID),
            KryonExecutionConfig.builder().executionId("r").build());
    kryonExecutor.close();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("l1:l2:l3:l4:final");
  }

  private static LogicDefinition<FacetsFromRequest> newFacetsFromRequestLogic(
      VajramID vajramID, Set<? extends Facet> allFacets) {
    return new LogicDefinition<>(
        new KryonLogicId(vajramID, vajramID.id() + ":facetsFromRequest"),
        request ->
            new FacetValuesMapBuilder(
                (SimpleRequestBuilder<Object>) request._asBuilder(), allFacets, vajramID));
  }

  @NonNull
  private static LogicDefinition<CreateNewRequest> newCreateNewRequestLogic(
      VajramID vajramID, Set<SimpleFacet> inputDefs) {
    return new LogicDefinition<>(
        new KryonLogicId(vajramID, vajramID.id() + ":newRequest"),
        () -> new SimpleRequestBuilder<>(inputDefs, vajramID));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    VajramID dep1 =
        vajramID(
            "executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce_dep1");
    kryonDefinitionRegistry.newVajramKryonDefinition(
        dep1,
        emptySet(),
        newComputeLogic(dep1, emptySet(), dependencyValues -> "l1").kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(dep1, emptySet()),
        newFacetsFromRequestLogic(dep1, emptySet()),
        _graphExecData ->
            _graphExecData
                .executionItems()
                .forEach(e -> _graphExecData.communicationFacade().executeOutputLogic(e)),
        emptyTags());

    VajramID dep2 =
        vajramID(
            "executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce_dep2");
    kryonDefinitionRegistry.newVajramKryonDefinition(
        dep2,
        emptySet(),
        newComputeLogic(dep2, emptySet(), dependencyValues -> "l2").kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(dep2, emptySet()),
        newFacetsFromRequestLogic(dep2, emptySet()),
        _graphExecData ->
            _graphExecData
                .executionItems()
                .forEach(e -> _graphExecData.communicationFacade().executeOutputLogic(e)),
        emptyTags());

    LongAdder numberOfExecutions = new LongAdder();
    String kryonName =
        "executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce_final";
    Set<Facet> allFacets = Set.of(dependency(1), dependency(2));
    VajramID vajramID =
        kryonDefinitionRegistry
            .newVajramKryonDefinition(
                kryonName,
                allFacets,
                newComputeLogic(
                        kryonName,
                        allFacets,
                        facets -> {
                          numberOfExecutions.increment();
                          return ((FacetValuesMap) facets)
                                  ._getDepResponses(1).requestResponsePairs().stream()
                                      .map(RequestResponse::response)
                                      .iterator()
                                      .next()
                                      .valueOpt()
                                      .orElseThrow()
                              + ":"
                              + ((FacetValuesMap) facets)
                                  ._getDepResponses(2).requestResponsePairs().stream()
                                      .map(RequestResponse::response)
                                      .iterator()
                                      .next()
                                      .valueOpt()
                                      .orElseThrow()
                              + ":final";
                        })
                    .kryonLogicId(),
                ImmutableMap.of(
                    dependency(1), new VajramID(dep1), dependency(2), new VajramID(dep2)),
                ImmutableMap.of(),
                newCreateNewRequestLogic(kryonName, emptySet()),
                newFacetsFromRequestLogic(kryonName, allFacets),
                ElementTags.of(Creator.create()))
            .vajramID();
    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(vajramID),
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
    String kryonName = "shutdown_preventsNewExecutionRequests";
    assertThrows(
        Exception.class,
        () ->
            kryonExecutor.executeKryon(
                SimpleImmutRequest.empty(new VajramID(kryonName)),
                KryonExecutionConfig.builder().executionId("req_1").build()));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void shutdownNow_terminatesPendingWork(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
    VajramKryonDefinition n1 =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            "n1",
            emptySet(),
            newComputeLogic("n1_logic", emptySet(), dependencyValues -> "dependency_value")
                .kryonLogicId(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            newCreateNewRequestLogic("n1_logic", emptySet()),
            newFacetsFromRequestLogic("n1_logic", emptySet()),
            emptyTags());

    CountDownLatch countDownLatch = new CountDownLatch(1);
    Set<SimpleDep> allFacets = Set.of(dependency(1));
    VajramKryonDefinition n2 =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            "n2",
            allFacets,
            newIoLogic(
                    "n2_logic",
                    allFacets,
                    facets ->
                        runAsync(
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
                                (unused, throwable) ->
                                    ((FacetValuesMap) facets)
                                            ._getDepResponses(1).requestResponsePairs().stream()
                                                .map(RequestResponse::response)
                                                .iterator()
                                                .next()
                                                .valueOpt()
                                                .orElseThrow()
                                        + ":computed_value"))
                .kryonLogicId(),
            ImmutableMap.of(dependency(1), n1.vajramID()),
            ImmutableMap.of(),
            newCreateNewRequestLogic("n1_logic", emptySet()),
            newFacetsFromRequestLogic("n1_logic", allFacets),
            ElementTags.of(List.of(InvocableOutsideGraph.Creator.create())));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(n2.vajramID()),
            KryonExecutionConfig.builder().executionId("r1").build());
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

  private <T> OutputLogicDefinition<T> newComputeLogic(
      VajramID kryonId, Set<? extends Facet> usedFacets, Function<FacetValues, T> logic) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(
            new KryonLogicId(kryonId, kryonId.id()),
            usedFacets,
            input ->
                new OutputLogicExecutionResults<>(
                    input.facetValues().stream()
                        .collect(
                            toImmutableMap(
                                identity(),
                                facetValues ->
                                    computeErrableFrom(logic).apply(facetValues).toFuture()))),
            emptyTags());

    logicDefinitionRegistry.addOutputLogic(def);
    return def;
  }

  private <T> OutputLogicDefinition<T> newIoLogic(
      String kryonId,
      Set<? extends Facet> inputs,
      Function<FacetValues, CompletableFuture<T>> logic) {
    IOLogicDefinition<T> def =
        new IOLogicDefinition<>(
            new KryonLogicId(new VajramID(kryonId), kryonId),
            inputs,
            input ->
                new OutputLogicExecutionResults<>(
                    input.facetValues().stream().collect(toImmutableMap(identity(), logic))),
            emptyTags());

    logicDefinitionRegistry.addOutputLogic(def);
    return def;
  }

  private KryonExecutor getKryonExecutor(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    var config =
        KryonExecutorConfig.builder()
            .executorService(executorLease.get())
            .kryonExecStrategy(kryonExecStrategy)
            .graphTraversalStrategy(graphTraversalStrategy)
            .configureWith(requestLevelCache)
            .executorId("test")
            .build();
    return new KryonExecutor(kryonDefinitionRegistry, config);
  }

  public static Stream<Arguments> executorConfigsToTest() {
    return Stream.of(Arguments.of(BATCH, DEPTH), Arguments.of(BATCH, BREADTH));
  }
}

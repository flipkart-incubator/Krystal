package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.data.Errable.computeErrableFrom;
import static com.flipkart.krystal.data.Errable.errableFrom;
import static com.flipkart.krystal.data.Errable.withValue;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.BREADTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.DIRECT;
import static com.flipkart.krystal.krystex.testutils.SimpleFacet.dependency;
import static com.flipkart.krystal.krystex.testutils.SimpleFacet.input;
import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static java.util.Collections.emptySet;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
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
import com.flipkart.krystal.krystex.testutils.SimpleRequestBuilder;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.ArrayList;
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
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
@ParameterizedClass
@MethodSource("executorConfigsToTest")
@SuppressWarnings("unchecked")
class KryonExecutorTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(1);
  private static SingleThreadExecutorsPool EXEC_POOL;

  private final KryonExecStrategy kryonExecStrategy;
  private final GraphTraversalStrategy graphTraversalStrategy;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("KryonExecutorTest", 4);
  }

  private Lease<SingleThreadExecutor> executorLease;
  private KryonExecutor kryonExecutor;
  private KryonDefinitionRegistry kryonDefinitionRegistry;
  private LogicDefinitionRegistry logicDefinitionRegistry;
  private RequestLevelCache requestLevelCache;

  public KryonExecutorTest(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    this.kryonExecStrategy = kryonExecStrategy;
    this.graphTraversalStrategy = graphTraversalStrategy;
  }

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    this.requestLevelCache = new RequestLevelCache();
    this.logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy);
  }

  @AfterEach
  void tearDown() {
    Optional.ofNullable(kryonExecutor).ifPresent(KryonExecutor::close);
    executorLease.close();
  }

  /** Executing same kryon multiple times in a single execution */
  @Test
  void multiRequestExecution() {
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
            _graphExecData -> _graphExecData.communicationFacade().executeOutputLogic(),
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

  @Test
  void multiRequestExecutionWithNullRequestId() {
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
            _graphExecData -> _graphExecData.communicationFacade().executeOutputLogic(),
            ElementTags.of(InvocableOutsideGraph.Creator.create()));

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

  @Test
  void requestExecution_noDependencies_success() {
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
            _graphExecData -> _graphExecData.communicationFacade().executeOutputLogic(),
            ElementTags.of(InvocableOutsideGraph.Creator.create()));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(kryonDefinition.vajramID()),
            KryonExecutionConfig.builder().executionId("req_1").build());
    kryonExecutor.close();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("computed_value");
  }

  @Test
  void requestExecution_unboundInputs_success() {
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
                                    ((FacetValuesMap) facets)._getOne2OneResponse(1).valueOrThrow(),
                                    ((FacetValuesMap) facets)._getOne2OneResponse(2).valueOrThrow(),
                                    ((FacetValuesMap) facets)
                                        ._getOne2OneResponse(3)
                                        .valueOrThrow()))
                    .kryonLogicId(),
                ImmutableMap.of(),
                newCreateNewRequestLogic(kryonName, inputDefs),
                newFacetsFromRequestLogic(kryonName, inputDefs),
                _graphExecData -> _graphExecData.communicationFacade().executeOutputLogic(),
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

  @Test
  void requestExecution_singleDependency_success() {
    VajramID n1ID = vajramID("n1");
    kryonDefinitionRegistry.newVajramKryonDefinition(
        n1ID,
        emptySet(),
        newComputeLogic(n1ID, emptySet(), dependencyValues -> "dependency_value").kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(n1ID, emptySet()),
        newFacetsFromRequestLogic(n1ID, emptySet()),
        _graphExecData -> _graphExecData.communicationFacade().executeOutputLogic(),
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
                        ((FacetValuesMap) facets)._getOne2OneResponse(1).valueOrThrow()
                            + ":computed_value")
                .kryonLogicId(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(n2ID, emptySet()),
            newFacetsFromRequestLogic(n2ID, allFacets),
            _graphExecData -> {
              List<CompletableFuture<Object>> list = new ArrayList<>();
              for (ExecutionItem executionItem : _graphExecData.executionItems()) {
                CompletableFuture<Object> n1Result = new CompletableFuture<>();
                SimpleRequestBuilder<Object> n1Req = new SimpleRequestBuilder<>(Set.of(), n1ID);
                _graphExecData
                    .communicationFacade()
                    .triggerDependency(
                        n1DependsOnN1, List.of(new RequestResponseFuture<>(n1Req, n1Result)));
                list.add(
                    n1Result.whenComplete(
                        (o, throwable) ->
                            ((FacetValuesMapBuilder) executionItem.facetValues()._asBuilder())
                                ._set(
                                    n1DependsOnN1.id(),
                                    new RequestResponse(n1Req, errableFrom(o, throwable)))));
              }
              CompletableFuture.allOf(list.toArray(CompletableFuture[]::new))
                  .whenComplete(
                      (unused, throwable) ->
                          _graphExecData.communicationFacade().executeOutputLogic());
            },
            ElementTags.of(InvocableOutsideGraph.Creator.create()));

    CompletableFuture<?> future =
        kryonExecutor.executeKryon(
            n2.createNewRequest().logic().newRequestBuilder()._build(),
            KryonExecutionConfig.builder().executionId("r1").build());
    kryonExecutor.close();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("dependency_value:computed_value");
  }

  @Test
  void requestExecution_multiLevelDependencies_success() {
    VajramID l1Dep = vajramID("requestExecution_multiLevelDependencies_level1");
    kryonDefinitionRegistry.newVajramKryonDefinition(
        l1Dep,
        emptySet(),
        newComputeLogic(l1Dep, emptySet(), facets -> "l1").kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(l1Dep, emptySet()),
        newFacetsFromRequestLogic(l1Dep, emptySet()),
        _graphExecData -> _graphExecData.communicationFacade().executeOutputLogic(),
        emptyTags());

    VajramID l2Dep = vajramID("requestExecution_multiLevelDependencies_level2");
    Dependency l2DependsOnL1 = dependency(1, l2Dep, l1Dep);
    Set<Dependency> l2Facets = Set.of(l2DependsOnL1);
    kryonDefinitionRegistry.newVajramKryonDefinition(
        l2Dep,
        l2Facets,
        newComputeLogic(
                l2Dep,
                l2Facets,
                facets -> ((FacetValuesMap) facets)._getOne2OneResponse(1).valueOrThrow() + ":l2")
            .kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(l2Dep, emptySet()),
        newFacetsFromRequestLogic(l2Dep, l2Facets),
        _graphExecData -> {
          List<CompletableFuture<Object>> list = new ArrayList<>();
          for (ExecutionItem executionItem : _graphExecData.executionItems()) {
            CompletableFuture<Object> l1Result = new CompletableFuture<>();
            SimpleRequestBuilder<Object> l1Req = new SimpleRequestBuilder<>(Set.of(), l1Dep);
            _graphExecData
                .communicationFacade()
                .triggerDependency(
                    l2DependsOnL1, List.of(new RequestResponseFuture<>(l1Req, l1Result)));
            list.add(
                l1Result.whenComplete(
                    (result, throwable) ->
                        ((FacetValuesMapBuilder) executionItem.facetValues()._asBuilder())
                            ._set(
                                l2DependsOnL1.id(),
                                new RequestResponse(
                                    l1Req, Errable.errableFrom(result, throwable)))));
          }
          CompletableFuture.allOf(list.toArray(CompletableFuture[]::new))
              .whenComplete(
                  (unused, throwable) -> _graphExecData.communicationFacade().executeOutputLogic());
        },
        emptyTags());

    VajramID l3Dep = vajramID("requestExecution_multiLevelDependencies_level3");
    Dependency l3DependsOnL2 = dependency(1, l3Dep, l2Dep);
    Set<Dependency> l3Facets = Set.of(l3DependsOnL2);

    kryonDefinitionRegistry.newVajramKryonDefinition(
        l3Dep,
        l3Facets,
        newComputeLogic(
                l3Dep,
                l3Facets,
                facets -> ((FacetValuesMap) facets)._getOne2OneResponse(1).valueOrThrow() + ":l3")
            .kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(l3Dep, emptySet()),
        newFacetsFromRequestLogic(l3Dep, l3Facets),
        _graphExecData -> {
          List<CompletableFuture<Object>> list = new ArrayList<>();
          for (ExecutionItem executionItem : _graphExecData.executionItems()) {
            CompletableFuture<Object> l2Result = new CompletableFuture<>();
            SimpleRequestBuilder<Object> l2Req = new SimpleRequestBuilder<>(Set.of(), l2Dep);
            _graphExecData
                .communicationFacade()
                .triggerDependency(
                    l3DependsOnL2, List.of(new RequestResponseFuture<>(l2Req, l2Result)));
            list.add(
                l2Result.whenComplete(
                    (o, throwable) ->
                        ((FacetValuesMapBuilder) executionItem.facetValues()._asBuilder())
                            ._set(
                                1, new RequestResponse(l2Req, Errable.errableFrom(o, throwable)))));
          }
          CompletableFuture.allOf(list.toArray(CompletableFuture[]::new))
              .whenComplete(
                  (unused, throwable) -> _graphExecData.communicationFacade().executeOutputLogic());
        },
        emptyTags());

    VajramID l4Dep = vajramID("requestExecution_multiLevelDependencies_level4");
    Dependency l4DepOnL3 = dependency(1, l4Dep, l3Dep);
    Set<Facet> l4Facets = Set.of(l4DepOnL3);
    kryonDefinitionRegistry.newVajramKryonDefinition(
        l4Dep,
        l4Facets,
        newComputeLogic(
                l4Dep,
                l4Facets,
                facets -> ((FacetValuesMap) facets)._getOne2OneResponse(1).valueOrThrow() + ":l4")
            .kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(l4Dep, emptySet()),
        newFacetsFromRequestLogic(l4Dep, l4Facets),
        _graphExecData -> {
          List<CompletableFuture<Object>> list = new ArrayList<>();
          for (ExecutionItem executionItem : _graphExecData.executionItems()) {
            CompletableFuture<Object> l3Result = new CompletableFuture<>();
            SimpleRequestBuilder<Object> l3Req = new SimpleRequestBuilder<>(Set.of(), l3Dep);
            _graphExecData
                .communicationFacade()
                .triggerDependency(
                    l4DepOnL3, List.of(new RequestResponseFuture<>(l3Req, l3Result)));
            list.add(
                l3Result.whenComplete(
                    (o, throwable) ->
                        ((FacetValuesMapBuilder) executionItem.facetValues()._asBuilder())
                            ._set(
                                l4DepOnL3.id(),
                                new RequestResponse(l3Req, errableFrom(o, throwable)))));
          }
          CompletableFuture.allOf(list.toArray(CompletableFuture[]::new))
              .whenComplete(
                  (unused, throwable) -> _graphExecData.communicationFacade().executeOutputLogic());
        },
        ElementTags.of(List.of(InvocableOutsideGraph.Creator.create())));

    VajramID finalKryon = vajramID("requestExecution_multiLevelDependencies_final");
    Dependency finalDepOnL4 = dependency(1, finalKryon, l4Dep);
    Set<Facet> finalFacets = Set.of(finalDepOnL4);
    kryonDefinitionRegistry.newVajramKryonDefinition(
        finalKryon,
        finalFacets,
        newComputeLogic(
                finalKryon,
                finalFacets,
                facets ->
                    ((FacetValuesMap) facets)._getOne2OneResponse(1).valueOrThrow() + ":final")
            .kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(finalKryon, emptySet()),
        newFacetsFromRequestLogic(finalKryon, finalFacets),
        _graphExecData -> {
          List<CompletableFuture<Object>> list = new ArrayList<>();
          for (ExecutionItem executionItem : _graphExecData.executionItems()) {
            CompletableFuture<Object> l4Result = new CompletableFuture<>();
            SimpleRequestBuilder<Object> l4Req = new SimpleRequestBuilder<>(Set.of(), l4Dep);
            _graphExecData
                .communicationFacade()
                .triggerDependency(
                    finalDepOnL4, List.of(new RequestResponseFuture<>(l4Req, l4Result)));
            list.add(
                l4Result.whenComplete(
                    (o, throwable) ->
                        ((FacetValuesMapBuilder) executionItem.facetValues()._asBuilder())
                            ._set(
                                finalDepOnL4.id(),
                                new RequestResponse(l4Req, errableFrom(o, throwable)))));
          }
          CompletableFuture.allOf(list.toArray(CompletableFuture[]::new))
              .whenComplete(
                  (unused, throwable) -> _graphExecData.communicationFacade().executeOutputLogic());
        },
        ElementTags.of(InvocableOutsideGraph.Creator.create()));
    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(finalKryon),
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

  @Test
  void executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce() {
    VajramID dep1ID =
        vajramID(
            "executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce_dep1");
    kryonDefinitionRegistry.newVajramKryonDefinition(
        dep1ID,
        emptySet(),
        newComputeLogic(dep1ID, emptySet(), dependencyValues -> "l1").kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(dep1ID, emptySet()),
        newFacetsFromRequestLogic(dep1ID, emptySet()),
        _graphExecData -> _graphExecData.communicationFacade().executeOutputLogic(),
        emptyTags());

    VajramID dep2ID =
        vajramID(
            "executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce_dep2");
    kryonDefinitionRegistry.newVajramKryonDefinition(
        dep2ID,
        emptySet(),
        newComputeLogic(dep2ID, emptySet(), dependencyValues -> "l2").kryonLogicId(),
        ImmutableMap.of(),
        newCreateNewRequestLogic(dep2ID, emptySet()),
        newFacetsFromRequestLogic(dep2ID, emptySet()),
        _graphExecData -> _graphExecData.communicationFacade().executeOutputLogic(),
        emptyTags());

    LongAdder numberOfExecutions = new LongAdder();
    VajramID kryonName =
        vajramID(
            "executeKryon_dependenciesWithReturnInstantly_executeComputeExecutedExactlyOnce_final");
    Dependency dep1 = dependency(1, kryonName, dep1ID);
    Dependency dep2 = dependency(2, kryonName, dep2ID);
    Set<Facet> allFacets = Set.of(dep1, dep2);
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
                          return ((FacetValuesMap) facets)._getOne2OneResponse(1).valueOrThrow()
                              + ":"
                              + ((FacetValuesMap) facets)._getOne2OneResponse(2).valueOrThrow()
                              + ":final";
                        })
                    .kryonLogicId(),
                ImmutableMap.of(),
                newCreateNewRequestLogic(kryonName, emptySet()),
                newFacetsFromRequestLogic(kryonName, allFacets),
                _graphExecData -> {
                  List<CompletableFuture<?>> list = new ArrayList<>();
                  for (ExecutionItem executionItem : _graphExecData.executionItems()) {
                    CompletableFuture<@Nullable Object> dep1Future = new CompletableFuture<>();
                    CompletableFuture<@Nullable Object> dep2Future = new CompletableFuture<>();
                    SimpleRequestBuilder<Object> dep1Req =
                        new SimpleRequestBuilder<>(Set.of(), dep1ID);
                    SimpleRequestBuilder<Object> dep2Req =
                        new SimpleRequestBuilder<>(Set.of(), dep2ID);
                    dep1Future.whenComplete(
                        (o, throwable) ->
                            ((FacetValuesMapBuilder) executionItem.facetValues()._asBuilder())
                                ._set(
                                    dep1.id(),
                                    new RequestResponse(dep1Req, errableFrom(o, throwable))));
                    dep2Future.whenComplete(
                        (o, throwable) ->
                            ((FacetValuesMapBuilder) executionItem.facetValues()._asBuilder())
                                ._set(
                                    dep2.id(),
                                    new RequestResponse(dep2Req, errableFrom(o, throwable))));
                    _graphExecData
                        .communicationFacade()
                        .triggerDependency(
                            dep1,
                            List.of(
                                new RequestResponseFuture<Request<Object>, Object>(
                                    dep1Req, dep1Future)));
                    _graphExecData
                        .communicationFacade()
                        .triggerDependency(
                            dep2,
                            List.of(
                                new RequestResponseFuture<Request<Object>, Object>(
                                    dep2Req, dep2Future)));
                    list.add(
                        CompletableFuture.allOf(dep1Future, dep2Future)
                            .whenComplete(
                                (unused, throwable) -> {
                                  FacetValuesMapBuilder f =
                                      (FacetValuesMapBuilder)
                                          executionItem.facetValues()._asBuilder();
                                  f._set(
                                      dep1.id(),
                                      new RequestResponse(
                                          dep1Req, errableFrom(dep1Future.join(), throwable)));
                                  f._set(
                                      dep2.id(),
                                      new RequestResponse(
                                          dep2Req, errableFrom(dep2Future.join(), throwable)));
                                }));
                  }
                  CompletableFuture.allOf(list.toArray(new CompletableFuture<?>[0]))
                      .whenComplete(
                          (unused, throwable) ->
                              _graphExecData.communicationFacade().executeOutputLogic());
                },
                ElementTags.of(InvocableOutsideGraph.Creator.create()))
            .vajramID();
    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(vajramID),
            KryonExecutionConfig.builder().executionId("r").build());
    kryonExecutor.close();
    assertThat(future).succeedsWithin(TIMEOUT).isEqualTo("l1:l2:final");
    assertThat(numberOfExecutions.sum()).isEqualTo(1);
  }

  @Test
  void close_preventsNewExecutionRequests() {
    kryonExecutor.close();
    String kryonName = "shutdown_preventsNewExecutionRequests";
    assertThrows(
        Exception.class,
        () ->
            kryonExecutor.executeKryon(
                SimpleImmutRequest.empty(new VajramID(kryonName)),
                KryonExecutionConfig.builder().executionId("req_1").build()));
  }

  @Test
  void shutdownNow_terminatesPendingWork() {
    VajramID n1ID = vajramID("n1");
    VajramKryonDefinition n1 =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            n1ID,
            emptySet(),
            newComputeLogic(n1ID, emptySet(), dependencyValues -> "dependency_value")
                .kryonLogicId(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(n1ID, emptySet()),
            newFacetsFromRequestLogic(n1ID, emptySet()),
            _graphExecData -> _graphExecData.communicationFacade().executeOutputLogic(),
            emptyTags());

    CountDownLatch countDownLatch = new CountDownLatch(1);
    VajramID n2Id = vajramID("n2");
    SimpleDep n2DepOnN1 = dependency(1, n2Id, n1ID);
    Set<SimpleDep> allFacets = Set.of(n2DepOnN1);
    VajramKryonDefinition n2 =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            n2Id,
            allFacets,
            newIoLogic(
                    n2Id,
                    allFacets,
                    facets ->
                        runAsync(
                                () -> {
                                  log.error("Running n2 vajram");
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
                                            ._getOne2OneResponse(1)
                                            .valueOpt()
                                            .orElseThrow()
                                        + ":computed_value"))
                .kryonLogicId(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(n1.vajramID(), emptySet()),
            newFacetsFromRequestLogic(n1.vajramID(), allFacets),
            _graphExecData -> _graphExecData.communicationFacade().executeOutputLogic(),
            ElementTags.of(List.of(InvocableOutsideGraph.Creator.create())));

    CompletableFuture<Object> future =
        kryonExecutor.executeKryon(
            SimpleImmutRequest.empty(n2.vajramID()),
            KryonExecutionConfig.builder().executionId("r1").build());
    kryonExecutor.close();
    kryonExecutor.shutdownNow();
    countDownLatch.countDown();
    assertThat(future)
        .failsWithin(TIMEOUT)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(RejectedExecutionException.class)
        .withMessage(
            "java.util.concurrent.RejectedExecutionException: Kryon Executor shutdown requested.");
  }

  private <T> OutputLogicDefinition<T> newComputeLogic(
      VajramID kryonId, Set<? extends Facet> usedFacets, Function<FacetValues, T> logic) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(
            new KryonLogicId(kryonId, kryonId.id() + "_logic"),
            usedFacets,
            input ->
                input
                    .facetValueResponses()
                    .forEach(
                        executionItem ->
                            linkFutures(
                                computeErrableFrom(logic)
                                    .apply(executionItem.facetValues())
                                    .toFuture(),
                                executionItem.response())),
            emptyTags());

    logicDefinitionRegistry.addOutputLogic(def);
    return def;
  }

  private <T> OutputLogicDefinition<T> newIoLogic(
      VajramID kryonId,
      Set<? extends Facet> inputs,
      Function<FacetValues, CompletableFuture<T>> logic) {
    IOLogicDefinition<T> def =
        new IOLogicDefinition<>(
            new KryonLogicId(kryonId, kryonId.id()),
            inputs,
            input ->
                input
                    .facetValueResponses()
                    .forEach(
                        executionItem ->
                            linkFutures(
                                logic.apply(executionItem.facetValues()),
                                executionItem.response())),
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
    return Stream.of(
        Arguments.of(BATCH, DEPTH), Arguments.of(BATCH, BREADTH), Arguments.of(DIRECT, DEPTH));
  }
}

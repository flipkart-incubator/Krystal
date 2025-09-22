package com.flipkart.krystal.krystex.caching;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.data.Errable.computeErrableFrom;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.BREADTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.DIRECT;
import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.core.OutputLogicExecutionResults;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.FacetsFromRequest;
import com.flipkart.krystal.krystex.testutils.FacetValuesMapBuilder;
import com.flipkart.krystal.krystex.testutils.SimpleFacet;
import com.flipkart.krystal.krystex.testutils.SimpleImmutRequest;
import com.flipkart.krystal.krystex.testutils.SimpleRequest;
import com.flipkart.krystal.krystex.testutils.SimpleRequestBuilder;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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

class RequestLevelCacheTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private static SingleThreadExecutorsPool EXEC_POOL;

  private KryonDefinitionRegistry kryonDefinitionRegistry;
  private LogicDefinitionRegistry logicDefinitionRegistry;
  private RequestLevelCache requestLevelCache;
  private KryonExecutor kryonExecutor;
  private Lease<SingleThreadExecutor> executorLease;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("RequestLevelCacheTest", 4);
  }

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.requestLevelCache = new RequestLevelCache();
    this.logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
    this.executorLease = EXEC_POOL.lease();
  }

  @AfterEach
  void tearDown() {
    Optional.ofNullable(kryonExecutor).ifPresent(KryonExecutor::close);
    executorLease.close();
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void multiRequestExecution_withCache_cacheHitSuccess(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    // This is redundant. This should Ideally move to a parametrized @BeforeEach method or after
    // parametrizing this at the test class level.
    // This is currently not supported in jupiter-junit:5.9.x.
    // It is planned to be supported in jupiter-junit:5.10
    // (Ref: https://github.com/junit-team/junit5/issues/878)
    // Move this to the @BeforeEach method after 5.10 is released.
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy, true);
    LongAdder adder = new LongAdder();
    VajramKryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            vajramID("kryon"),
            emptySet(),
            newComputeLogic(
                    vajramID("kryon"),
                    emptySet(),
                    dependencyValues -> {
                      adder.increment();
                      return "computed_value";
                    })
                .kryonLogicId(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(vajramID("kryon"), emptySet()),
            newFacetsFromRequestLogic(vajramID("kryon")),
            _graphExecData ->
                _graphExecData
                    .executionItems()
                    .forEach(e -> _graphExecData.communicationFacade().executeOutputLogic(e)),
            ElementTags.of(List.of(InvocableOutsideGraph.Creator.create())));
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
    switch (kryonExecStrategy) {
      case BATCH -> assertThat(adder.sum()).isEqualTo(1);
      case DIRECT -> assertThat(adder.sum()).isEqualTo(2);
    }
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void multiRequestExecution_withoutCache_cacheHitFailForBatch(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    // This is redundant. This should Ideally move to a parameterized @BeforeEach method or after
    // parametrizing this at the test class level.
    // This is currently not supported in jupiter-junit:5.9.x.
    // It is planned to be supported in jupiter-junit:5.10
    // (Ref: https://github.com/junit-team/junit5/issues/878)
    // Move this to the @BeforeEach method after 5.10 is released.
    this.kryonExecutor = getKryonExecutor(kryonExecStrategy, graphTraversalStrategy, false);
    LongAdder outptuLogicInvocationCount = new LongAdder();
    VajramKryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            vajramID("kryon"),
            emptySet(),
            newComputeLogic(
                    vajramID("kryonLogic"),
                    emptySet(),
                    dependencyValues -> {
                      outptuLogicInvocationCount.increment();
                      return "computed_value";
                    })
                .kryonLogicId(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(vajramID("kryon"), emptySet()),
            newFacetsFromRequestLogic(vajramID("kryon")),
            _graphExecData ->
                _graphExecData
                    .executionItems()
                    .forEach(e -> _graphExecData.communicationFacade().executeOutputLogic(e)),
            ElementTags.of(List.of(InvocableOutsideGraph.Creator.create())));

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
    assertThat(outptuLogicInvocationCount.sum()).isEqualTo(2);
  }

  private KryonExecutor getKryonExecutor(
      KryonExecStrategy kryonExecStrategy,
      GraphTraversalStrategy graphTraversalStrategy,
      boolean withCache) {
    KryonExecutorConfigBuilder configBuilder =
        KryonExecutorConfig.builder()
            .executorService(executorLease.get())
            .kryonExecStrategy(kryonExecStrategy)
            .graphTraversalStrategy(graphTraversalStrategy);
    if (withCache) {
      configBuilder.configureWith(requestLevelCache).build();
    }
    return new KryonExecutor(kryonDefinitionRegistry, configBuilder.executorId("test").build());
  }

  private <T> OutputLogicDefinition<T> newComputeLogic(
      VajramID kryonId, Set<Facet> inputs, Function<FacetValues, T> logic) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(
            new KryonLogicId(kryonId, kryonId.id()),
            inputs,
            input ->
                new OutputLogicExecutionResults<>(
                    input.facetValues().stream()
                        .collect(toImmutableMap(FacetValues::_build, computeErrableFrom(logic)))
                        .entrySet()
                        .stream()
                        .collect(toImmutableMap(Entry::getKey, e -> e.getValue().toFuture()))),
            emptyTags());

    logicDefinitionRegistry.addOutputLogic(def);
    return def;
  }

  @SuppressWarnings("unchecked")
  private static LogicDefinition<FacetsFromRequest> newFacetsFromRequestLogic(VajramID kryonName) {
    return new LogicDefinition<>(
        new KryonLogicId(kryonName, kryonName.id() + ":facetsFromRequest"),
        request ->
            new FacetValuesMapBuilder(
                ((SimpleRequest<Object>) request)._asBuilder(), Set.of(), kryonName));
  }

  @NonNull
  private static LogicDefinition<CreateNewRequest> newCreateNewRequestLogic(
      VajramID vajramID, Set<SimpleFacet> inputDefs) {
    return new LogicDefinition<>(
        new KryonLogicId(vajramID, vajramID.id() + ":newRequest"),
        () -> new SimpleRequestBuilder<>(inputDefs, vajramID));
  }

  public static Stream<Arguments> executorConfigsToTest() {
    return Stream.of(
        Arguments.of(BATCH, DEPTH), Arguments.of(BATCH, BREADTH), Arguments.of(DIRECT, DEPTH));
  }
}

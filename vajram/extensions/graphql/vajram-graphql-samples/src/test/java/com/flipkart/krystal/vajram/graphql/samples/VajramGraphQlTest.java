package com.flipkart.krystal.vajram.graphql.samples;

import static com.flipkart.krystal.vajram.graphql.samples.order.GetOrderSummary.UNIX_EPOCH_DATE;
import static com.flipkart.krystal.vajram.graphql.samples.order.GetOrderSummary.UNIX_EPOCH_DATE_TIME;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.KrystalExecutorConfig;
import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.kryon.VajramExecutionConfig;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor;
import com.flipkart.krystal.krystex.traits.PredicateDispatchUtil;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.vajram.graphql.api.execution.GraphQLQuery;
import com.flipkart.krystal.vajram.graphql.api.execution.GraphQlExecutionFacade;
import com.flipkart.krystal.vajram.graphql.api.schema.GraphQlInitializer;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate_Req;
import com.flipkart.krystal.vajram.graphql.client.GraphQlSpecRequest;
import com.flipkart.krystal.vajram.graphql.samples.client.DummyIdOnly;
import com.flipkart.krystal.vajram.graphql.samples.client.GetNameByStringOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetNameByStringOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetNameByStringOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetNameByStringVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetNameByValueOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetNameByValueOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetNameByValueOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetNameByValueVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderDummiesFanoutOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderDummiesFanoutOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderDummiesFanoutOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderDummiesFanoutVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderDummyFanoutOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderDummyFanoutOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderDummyFanoutOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderDummyFanoutVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderExecutionOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderExecutionOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderExecutionOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderExecutionVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderItemFanoutOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderItemFanoutOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderItemFanoutOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderItemFanoutVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderNoArgDummiesFanoutOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderNoArgDummiesFanoutOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderNoArgDummiesFanoutOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderNoArgDummiesFanoutVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithAliasedFragmentOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithAliasedFragmentOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithAliasedFragmentOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithAliasedFragmentVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithDummiesOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithDummiesOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithDummiesOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithDummiesVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithFragmentOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithFragmentOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithFragmentOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrderWithFragmentVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrdersAliasedOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrdersAliasedOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrdersAliasedOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrdersAliasedVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrdersWithFragmentOp;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrdersWithFragmentOp_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrdersWithFragmentOp_SpecReq;
import com.flipkart.krystal.vajram.graphql.samples.client.GetOrdersWithFragmentVariables_ImmutJson;
import com.flipkart.krystal.vajram.graphql.samples.query.Query_GQlAggr_Req;
import com.flipkart.krystal.vajram.graphql.samples.state.State;
import com.flipkart.krystal.vajram.json.Json;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VajramGraphQlTest {

  private static final Duration TEST_TIMEOUT = Duration.ofSeconds(1);

  private static SingleThreadExecutorsPool EXEC_POOL;
  private static GraphQL GRAPHQL;
  private KrystexGraphBuilder kGraph;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL =
        new SingleThreadExecutorsPool(
            "GraphQlSamplesE2ETest", Runtime.getRuntime().availableProcessors());
    GraphQlInitializer graphQlInitializer = new GraphQlInitializer();
    GRAPHQL = graphQlInitializer.getGraphQl();
  }

  @AfterAll
  static void afterAll() {
    EXEC_POOL.close();
  }

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    VajramGraph vGraph =
        VajramGraph.builder()
            .loadFromPackage(this.getClass().getPackage().getName())
            .loadFromPackage(GraphQlOperationAggregate.class.getPackageName())
            .build();
    this.kGraph = KrystexGraph.builder().vajramGraph(vGraph);
    kGraph.traitDispatchPolicies(
        new TraitDispatchPolicies(
            PredicateDispatchUtil.dispatchTrait(GraphQlOperationAggregate_Req.class, vGraph)
                .alwaysTo(Query_GQlAggr_Req.class)));
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void graphqlQueryExecution_succeeds() throws JsonProcessingException {
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetOrderExecutionVariables_ImmutJson variables =
          GetOrderExecutionVariables_ImmutJson._builder()
              .orderId("order1")
              .dummyId("dummy1")
              .userId("user1")
              ._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetOrderExecutionOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    @SuppressWarnings("unchecked")
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    GetOrderExecutionOp_ImmutJson result1 =
        Json.JSON_MAPPER.convertValue(queryData, GetOrderExecutionOp_ImmutJson.class);
    GetOrderExecutionOp response = result1;

    assertThat(response.order().orderItemNames()).isEqualTo(List.of("order1_1", "order1_2"));
    assertThat(response.order().nameString()).isEqualTo("testOrderName");
    assertThat(response.order().stateDuplicate()).isEqualTo(response.order().state());
    assertThat(response.order().typename()).isEqualTo("Order");
    assertThat(response.order().orderItemsCount()).isEqualTo(Long.MAX_VALUE);
    assertThat(response.order().orderPlacedAt()).isEqualTo(UNIX_EPOCH_DATE_TIME);
    assertThat(response.order().orderAcceptDate()).isEqualTo(UNIX_EPOCH_DATE);
    assertThat(response.dummy().typename()).isEqualTo("Dummy");
    assertThat(response.mostRecentOrder().orderItemNames())
        .isEqualTo(List.of("MostRecentOrderOf_user1_1", "MostRecentOrderOf_user1_2"));
    assertThat(response.typename()).isEqualTo("Query");

    System.out.println(
        Json.OBJECT_WRITER
            .withDefaultPrettyPrinter()
            .writeValueAsString(executionResult.toSpecification()));
  }

  @Test
  void inferIdFromArgs_withOptionalIdField_buildsIdentityFromArgs() throws JsonProcessingException {
    // `name` returns a `Name` type with an optional (`value`) and a mandatory (`string`)
    // @idField. @inferIdFromArgs must map every idField from the matching args, not just
    // non-null ones.
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetNameByValueVariables_ImmutJson variables =
          GetNameByValueVariables_ImmutJson._builder().value("v1").string("s1")._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetNameByValueOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    @SuppressWarnings("unchecked")
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    GetNameByValueOp_ImmutJson result1 =
        Json.JSON_MAPPER.convertValue(queryData, GetNameByValueOp_ImmutJson.class);
    GetNameByValueOp response = result1;
    assertThat(response.name().value()).isEqualTo("v1");
    assertThat(response.name().string()).isEqualTo("s1");
  }

  @Test
  void inferIdFromArgs_withOptionalIdFieldHavingNoArgAtAll_leavesItUnset()
      throws JsonProcessingException {
    // `nameByString` doesn't declare a `value` arg at all; Name's optional `value` @idField must
    // simply be left absent, not cause a build-time or run-time failure.
    CompletableFuture<ExecutionResult> future;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetNameByStringVariables_ImmutJson variables =
          GetNameByStringVariables_ImmutJson._builder().string("s1")._build();
      future =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetNameByStringOp_SpecReq.of(variables)));
    }
    assertThat(future).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = future.join();
    GetNameByStringOp response =
        Json.JSON_MAPPER.convertValue(executionResult.getData(), GetNameByStringOp_ImmutJson.class);
    assertThat(response.nameByString().value()).isNull();
    assertThat(response.nameByString().string()).isEqualTo("s1");
  }

  @Test
  void graphqlQueryWithQueryLevelAliases_succeeds() throws JsonProcessingException {
    // Two aliases for the same arg-bearing `order` field at query level, each with a different id
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetOrdersAliasedVariables_ImmutJson variables =
          GetOrdersAliasedVariables_ImmutJson._builder().o1Id("order1").o2Id("order2")._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetOrdersAliasedOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    @SuppressWarnings("unchecked")
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    GetOrdersAliasedOp_ImmutJson result1 =
        Json.JSON_MAPPER.convertValue(queryData, GetOrdersAliasedOp_ImmutJson.class);
    GetOrdersAliasedOp response = result1;

    // `s: state` alias resolves the `state` field under a different response key
    assertThat(response.o1().s())
        .isEqualTo(com.flipkart.krystal.vajram.graphql.samples.client.State.COMPLETED);
    assertThat(response.o1().orderItemNames()).isEqualTo(List.of("order1_1", "order1_2"));
    assertThat(response.o2().state())
        .isEqualTo(com.flipkart.krystal.vajram.graphql.samples.client.State.COMPLETED);
    assertThat(response.o2().orderItemNames()).isEqualTo(List.of("order2_1", "order2_2"));
  }

  @Test
  void graphqlQueryWithNestedArgBearingAliases_succeeds() throws JsonProcessingException {
    // Two aliases for the same arg-bearing `dummy(name)` field nested inside order
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetOrderWithDummiesVariables_ImmutJson variables =
          GetOrderWithDummiesVariables_ImmutJson._builder()
              .id("order1")
              .d1Name("foo")
              .d2Name("bar")
              ._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetOrderWithDummiesOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    @SuppressWarnings("unchecked")
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    GetOrderWithDummiesOp response =
        Json.JSON_MAPPER.convertValue(queryData, GetOrderWithDummiesOp_ImmutJson.class);

    // GetDummyIdForOrder ignores `name` arg, always returns orderId_dummy_1; both aliases map to
    // separate Dummy responses keyed by alias.
    assertThat(response.order().d1().dummyId()).isEqualTo("order1_dummy_1");
    assertThat(response.order().d2().dummyId()).isEqualTo("order1_dummy_1");
  }

  @Test
  void argBearingSingleFieldDataFetcherAliases_fansOutPerAlias() throws JsonProcessingException {
    // orderItemAt (scalar) and orderItemNamesFrom (list) are single-field @dataFetcher fields
    // with GraphQL arguments: each aliased invocation must fan out to its own vajram call and
    // get back a distinct, argument-specific response - not a shared/one-to-one response.
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetOrderItemFanoutVariables_ImmutJson variables =
          GetOrderItemFanoutVariables_ImmutJson._builder()
              .id("order1")
              .i1Index(1)
              .i2Index(2)
              .f1Offset(10)
              .f2Offset(20)
              ._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetOrderItemFanoutOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    Map<String, Object> queryData = requireNonNull(result.join().getData());
    GetOrderItemFanoutOp result1 =
        Json.JSON_MAPPER.convertValue(queryData, GetOrderItemFanoutOp_ImmutJson.class);
    GetOrderItemFanoutOp response = result1;

    assertThat(response.order().i1()).isEqualTo("order1_item_1");
    assertThat(response.order().i2()).isEqualTo("order1_item_2");
    assertThat(response.order().f1()).isEqualTo(List.of("order1_from_10_1", "order1_from_10_2"));
    assertThat(response.order().f2()).isEqualTo(List.of("order1_from_20_1", "order1_from_20_2"));
  }

  @Test
  void argBearingListIdFetcherAliases_withSomeAliasesFailing_surfacesErrorsWithoutDroppingFields()
      throws JsonProcessingException {
    // `dummies` is an arg-bearing list @idFetcher field (GetDummyIdsWithArgs) whose id-fetcher
    // fans out one call per alias. One alias's id-fetcher call succeeds returning n ids, while a
    // sibling alias's id-fetcher call throws. A correct implementation must still surface a
    // result (null + a GraphQL error) for the failing alias - it must not silently disappear from
    // the response, and it must not corrupt data returned for the succeeding alias.
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetOrderDummiesFanoutVariables_ImmutJson variables =
          GetOrderDummiesFanoutVariables_ImmutJson._builder()
              .id("order1")
              .okFilter(true)
              .okPreferredType("fine")
              .okCount(2)
              .badFilter(true)
              .badPreferredType("boom")
              .badCount(2)
              ._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetOrderDummiesFanoutOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    GetOrderDummiesFanoutOp response =
        Json.JSON_MAPPER.convertValue(queryData, GetOrderDummiesFanoutOp_ImmutJson.class);

    // The successful alias must retain its own 2 ids, unaffected by the sibling failure.
    assertThat(response.order().ok()).isNotNull();
    assertThat(response.order().ok().stream().map(DummyIdOnly::dummyId).toList())
        .isEqualTo(List.of("order1_dummy_1", "order1_dummy_2"));

    // The failing alias resolves to `null` (per GraphQL spec: a nullable field that errors
    // resolves to `null` with an entry in `errors`), not an exception.
    assertThat(response.order().bad()).isNull();
    assertThat(executionResult.getErrors().isEmpty()).isFalse();
  }

  @Test
  void argBearingNonListIdFetcherAliases_withOneAliasFailing_surfacesErrorWithoutDroppingField()
      throws JsonProcessingException {
    // `dummy` is an arg-bearing non-list @idFetcher field (GetDummyIdForOrder) whose id-fetcher
    // fans out one call per alias. One alias's id-fetcher call succeeds, a sibling alias's call
    // throws. The failing alias must still show up as `null` + a GraphQL error, and the
    // succeeding alias's data must be unaffected.
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetOrderDummyFanoutVariables_ImmutJson variables =
          GetOrderDummyFanoutVariables_ImmutJson._builder()
              .id("order1")
              .okName("fine")
              .badName("boom")
              ._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetOrderDummyFanoutOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    GetOrderDummyFanoutOp response =
        Json.JSON_MAPPER.convertValue(queryData, GetOrderDummyFanoutOp_ImmutJson.class);

    assertThat(response.order().ok()).isNotNull();
    assertThat(response.order().ok().dummyId()).isEqualTo("order1_dummy_1");

    assertThat(response.order().bad()).isNull();
    assertThat(executionResult.getErrors().isEmpty()).isFalse();
  }

  @Test
  void argLessListIdFetcherAliases_whenIdFetcherFails_surfacesSameErrorForAllAliases()
      throws JsonProcessingException {
    // `noArgDummies` is an arg-less list @idFetcher field (GetDummyIds). With no arguments,
    // the id-fetcher is called exactly once for the whole field (no per-alias fanout is
    // possible) - so if it fails, every alias of `noArgDummies` must see the same null + error,
    // not silently disappear.
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetOrderNoArgDummiesFanoutVariables_ImmutJson variables =
          GetOrderNoArgDummiesFanoutVariables_ImmutJson._builder().id("orderBadDummies")._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetOrderNoArgDummiesFanoutOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    GetOrderNoArgDummiesFanoutOp_ImmutJson result1 =
        Json.JSON_MAPPER.convertValue(queryData, GetOrderNoArgDummiesFanoutOp_ImmutJson.class);
    GetOrderNoArgDummiesFanoutOp response = result1;

    assertThat(response.order().a1()).isNull();
    assertThat(response.order().a2()).isNull();
    assertThat(executionResult.getErrors().isEmpty()).isFalse();
  }

  @Test
  void graphqlQueryWithNamedFragment_noAliases_succeeds() throws JsonProcessingException {
    // A named fragment (`OrderFieldsFragment`, spread via `OrderWithFragment`), with no aliases
    // anywhere in the query.
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetOrderWithFragmentVariables_ImmutJson variables =
          GetOrderWithFragmentVariables_ImmutJson._builder().id("order1")._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetOrderWithFragmentOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    Map<String, Object> queryData = requireNonNull(result.join().getData());
    GetOrderWithFragmentOp response =
        Json.JSON_MAPPER.convertValue(queryData, GetOrderWithFragmentOp_ImmutJson.class);

    assertThat(response.order().state())
        .isEqualTo(com.flipkart.krystal.vajram.graphql.samples.client.State.COMPLETED);
    assertThat(response.order().orderItemNames()).isEqualTo(List.of("order1_1", "order1_2"));
  }

  @Test
  void graphqlQueryWithNamedFragment_underQueryLevelAliases_succeeds()
      throws JsonProcessingException {
    // The same named fragment is spread under two differently-aliased `order` selections. Each
    // alias must resolve the fragment's fields against its own argument-specific order.
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetOrdersWithFragmentVariables_ImmutJson variables =
          GetOrdersWithFragmentVariables_ImmutJson._builder()
              .o1Id("order1")
              .o2Id("order2")
              ._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetOrdersWithFragmentOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    Map<String, Object> queryData = requireNonNull(result.join().getData());
    GetOrdersWithFragmentOp response =
        Json.JSON_MAPPER.convertValue(queryData, GetOrdersWithFragmentOp_ImmutJson.class);

    assertThat(response.o1().state())
        .isEqualTo(com.flipkart.krystal.vajram.graphql.samples.client.State.COMPLETED);
    assertThat(response.o1().orderItemNames()).isEqualTo(List.of("order1_1", "order1_2"));
    assertThat(response.o2().state())
        .isEqualTo(com.flipkart.krystal.vajram.graphql.samples.client.State.COMPLETED);
    assertThat(response.o2().orderItemNames()).isEqualTo(List.of("order2_1", "order2_2"));
  }

  @Test
  void graphqlQueryWithNamedFragment_havingFieldAliasesInsideFragment_succeeds()
      throws JsonProcessingException {
    // Fields aliased *inside* the fragment definition itself (`OrderFieldsAliasedFragment`, not
    // at the spread site) must resolve under their aliased response keys.
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      GetOrderWithAliasedFragmentVariables_ImmutJson variables =
          GetOrderWithAliasedFragmentVariables_ImmutJson._builder().id("order1")._build();
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  toGraphQLQuery(GetOrderWithAliasedFragmentOp_SpecReq.of(variables)));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    Map<String, Object> queryData = requireNonNull(result.join().getData());
    GetOrderWithAliasedFragmentOp response =
        Json.JSON_MAPPER.convertValue(queryData, GetOrderWithAliasedFragmentOp_ImmutJson.class);

    assertThat(response.order().s())
        .isEqualTo(com.flipkart.krystal.vajram.graphql.samples.client.State.COMPLETED);
    assertThat(response.order().names()).isEqualTo(List.of("order1_1", "order1_2"));
  }

  @Test
  void graphqlQueryWithInlineFragment_succeeds() {
    // An inline fragment (`... on Order { ... }`), with no named fragment and no aliases.
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  new GraphQLQuery(
                      """
                      query {
                        order(id: "order1") {
                          ... on Order {
                            state
                            orderItemNames
                          }
                        }
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    Map<String, Object> queryData = requireNonNull(result.join().getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> orderData = requireNonNull((Map<String, Object>) queryData.get("order"));

    assertThat(orderData.get("state")).isEqualTo(State.COMPLETED);
    assertThat(orderData.get("orderItemNames")).isEqualTo(List.of("order1_1", "order1_2"));
  }

  /**
   * Converts a generated {@link GraphQlSpecRequest} into a {@link GraphQLQuery} for in-process
   * execution via {@link GraphQlExecutionFacade}. {@code req.variables()} is the raw variables
   * model instance (not a Map); it's converted here since all client models in {@code client/}
   * declare {@code @SupportedModelProtocol(Json.class)}.
   */
  private static GraphQLQuery toGraphQLQuery(GraphQlSpecRequest req)
      throws JsonProcessingException {
    @SuppressWarnings("unchecked")
    Map<String, Object> variables =
        Json.OBJECT_READER
            .forType(Map.class)
            .readValue(Json.OBJECT_WRITER.writeValueAsString(req.variables()));
    return new GraphQLQuery(req.query(), variables);
  }

  private VajramKryonExecutor createExecutor() {
    return kGraph
        .build()
        .createExecutor(KrystalExecutorConfig.builder().executorService(executorLease.get()));
  }
}

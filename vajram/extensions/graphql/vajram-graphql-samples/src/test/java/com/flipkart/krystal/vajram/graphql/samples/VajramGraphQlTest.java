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
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  new GraphQLQuery(
                      """
                      query {
                        order(id: "order1") {
                          orderItemNames
                          nameString
                          state
                          stateDuplicate
                          orderPlacedAt
                          orderItemsCount
                          orderAcceptDate
                          __typename
                        }
                        dummy(dummyId: "dummy1") {
                          name
                          age
                          f1
                          __typename
                        }
                        mostRecentOrder(userId: "user1") {
                          orderItemNames
                        }
                        __typename
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    @SuppressWarnings("unchecked")
    Map<String, Object> queryData = requireNonNull((Map<String, Object>) executionResult.getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> orderData = requireNonNull((Map<String, Object>) queryData.get("order"));
    @SuppressWarnings("unchecked")
    Map<String, Object> dummyData = requireNonNull((Map<String, Object>) queryData.get("dummy"));
    @SuppressWarnings("unchecked")
    Map<String, Object> mostRecentOrderData =
        requireNonNull((Map<String, Object>) queryData.get("mostRecentOrder"));
    assertThat(orderData.get("orderItemNames")).isEqualTo(List.of("order1_1", "order1_2"));
    assertThat(orderData.get("nameString")).isEqualTo("testOrderName");
    assertThat(orderData.get("stateDuplicate")).isEqualTo(orderData.get("state"));
    assertThat(orderData.get("__typename")).isEqualTo("Order");
    assertThat(orderData.get("orderItemsCount")).isEqualTo(Long.MAX_VALUE);
    assertThat(orderData.get("orderPlacedAt")).isEqualTo(UNIX_EPOCH_DATE_TIME);
    assertThat(orderData.get("orderAcceptDate")).isEqualTo(UNIX_EPOCH_DATE);
    assertThat(dummyData.get("__typename")).isEqualTo("Dummy");
    assertThat(mostRecentOrderData.get("orderItemNames"))
        .isEqualTo(List.of("MostRecentOrderOf_user1_1", "MostRecentOrderOf_user1_2"));

    System.out.println(
        Json.OBJECT_WRITER
            .withDefaultPrettyPrinter()
            .writeValueAsString(executionResult.toSpecification()));
  }

  @Test
  void inferIdFromArgs_withOptionalIdField_buildsIdentityFromArgs() {
    // `name` returns a `Name` type with an optional (`value`) and a mandatory (`string`)
    // @idField. @inferIdFromArgs must map every idField from the matching args, not just
    // non-null ones.
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
                        name(value: "v1", string: "s1") {
                          value
                          string
                        }
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    @SuppressWarnings("unchecked")
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> nameData = requireNonNull((Map<String, Object>) queryData.get("name"));
    assertThat(nameData.get("value")).isEqualTo("v1");
    assertThat(nameData.get("string")).isEqualTo("s1");
  }

  @Test
  void inferIdFromArgs_withOptionalIdFieldHavingNoArgAtAll_leavesItUnset() {
    // `nameByString` doesn't declare a `value` arg at all; Name's optional `value` @idField must
    // simply be left absent, not cause a build-time or run-time failure.
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
                        nameByString(string: "s1") {
                          value
                          string
                        }
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    @SuppressWarnings("unchecked")
    Map<String, Object> queryData = requireNonNull((Map<String, Object>) executionResult.getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> nameData =
        requireNonNull((Map<String, Object>) queryData.get("nameByString"));
    assertThat(nameData.get("value")).isNull();
    assertThat(nameData.get("string")).isEqualTo("s1");
  }

  @Test
  void graphqlQueryWithQueryLevelAliases_succeeds() throws JsonProcessingException {
    // Two aliases for the same arg-bearing `order` field at query level, each with a different id
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
                        o1: order(id: "order1") {
                          s: state
                          orderItemNames
                        }
                        o2: order(id: "order2") {
                          state
                          orderItemNames
                        }
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    @SuppressWarnings("unchecked")
    Map<String, Object> queryData = requireNonNull((Map<String, Object>) executionResult.getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> o1Data = requireNonNull((Map<String, Object>) queryData.get("o1"));
    @SuppressWarnings("unchecked")
    Map<String, Object> o2Data = requireNonNull((Map<String, Object>) queryData.get("o2"));

    // `s: state` alias resolves the `state` field under a different response key
    assertThat(o1Data.get("s")).isEqualTo(State.COMPLETED);
    assertThat(o1Data.get("orderItemNames")).isEqualTo(List.of("order1_1", "order1_2"));
    assertThat(o2Data.get("state")).isEqualTo(State.COMPLETED);
    assertThat(o2Data.get("orderItemNames")).isEqualTo(List.of("order2_1", "order2_2"));
  }

  @Test
  void graphqlQueryWithNestedArgBearingAliases_succeeds() {
    // Two aliases for the same arg-bearing `dummy(name)` field nested inside order
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
                          d1: dummy(name: "foo") {
                            dummyId
                          }
                          d2: dummy(name: "bar") {
                            dummyId
                          }
                        }
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    @SuppressWarnings("unchecked")
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> orderData = requireNonNull((Map<String, Object>) queryData.get("order"));

    // Both aliases map to separate Dummy responses keyed by alias
    @SuppressWarnings("unchecked")
    Map<String, Object> d1Data = requireNonNull((Map<String, Object>) orderData.get("d1"));
    @SuppressWarnings("unchecked")
    Map<String, Object> d2Data = requireNonNull((Map<String, Object>) orderData.get("d2"));

    // GetDummyIdForOrder ignores `name` arg, always returns orderId_dummy_1
    assertThat(d1Data.get("dummyId")).isEqualTo("order1_dummy_1");
    assertThat(d2Data.get("dummyId")).isEqualTo("order1_dummy_1");
  }

  @Test
  void argBearingSingleFieldDataFetcherAliases_fansOutPerAlias() {
    // orderItemAt (scalar) and orderItemNamesFrom (list) are single-field @dataFetcher fields
    // with GraphQL arguments: each aliased invocation must fan out to its own vajram call and
    // get back a distinct, argument-specific response - not a shared/one-to-one response.
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
                          i1: orderItemAt(index: 1)
                          i2: orderItemAt(index: 2)
                          f1: orderItemNamesFrom(offset: 10)
                          f2: orderItemNamesFrom(offset: 20)
                        }
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    Map<String, Object> queryData = requireNonNull(result.join().getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> orderData = requireNonNull((Map<String, Object>) queryData.get("order"));

    assertThat(orderData.get("i1")).isEqualTo("order1_item_1");
    assertThat(orderData.get("i2")).isEqualTo("order1_item_2");
    assertThat(orderData.get("f1")).isEqualTo(List.of("order1_from_10_1", "order1_from_10_2"));
    assertThat(orderData.get("f2")).isEqualTo(List.of("order1_from_20_1", "order1_from_20_2"));
  }

  @Test
  void argBearingListIdFetcherAliases_withSomeAliasesFailing_surfacesErrorsWithoutDroppingFields() {
    // `dummies` is an arg-bearing list @idFetcher field (GetDummyIdsWithArgs) whose id-fetcher
    // fans out one call per alias. One alias's id-fetcher call succeeds returning n ids, while a
    // sibling alias's id-fetcher call throws. A correct implementation must still surface a
    // result (null + a GraphQL error) for the failing alias - it must not silently disappear from
    // the response, and it must not corrupt data returned for the succeeding alias.
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
                          ok: dummies(filter: true, preferredType: "fine", count: 2) {
                            dummyId
                          }
                          bad: dummies(filter: true, preferredType: "boom", count: 2) {
                            dummyId
                          }
                        }
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> orderData = requireNonNull((Map<String, Object>) queryData.get("order"));

    // The successful alias must retain its own 2 ids, unaffected by the sibling failure.
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> okData = (List<Map<String, Object>>) orderData.get("ok");
    assertThat(okData).isNotNull();
    assertThat(okData.stream().map(m -> m.get("dummyId")).toList())
        .isEqualTo(List.of("order1_dummy_1", "order1_dummy_2"));

    // The failing alias's key must still be present in the response (per GraphQL spec: a
    // nullable field that errors resolves to `null` with an entry in `errors`), not vanish.
    assertThat(orderData.containsKey("bad")).isTrue();
    assertThat(orderData.get("bad")).isNull();
    assertThat(executionResult.getErrors().isEmpty()).isFalse();
  }

  @Test
  void argBearingNonListIdFetcherAliases_withOneAliasFailing_surfacesErrorWithoutDroppingField() {
    // `dummy` is an arg-bearing non-list @idFetcher field (GetDummyIdForOrder) whose id-fetcher
    // fans out one call per alias. One alias's id-fetcher call succeeds, a sibling alias's call
    // throws. The failing alias must still show up as `null` + a GraphQL error, and the
    // succeeding alias's data must be unaffected.
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
                          ok: dummy(name: "fine") {
                            dummyId
                          }
                          bad: dummy(name: "boom") {
                            dummyId
                          }
                        }
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> orderData = requireNonNull((Map<String, Object>) queryData.get("order"));

    @SuppressWarnings("unchecked")
    Map<String, Object> okData = (Map<String, Object>) orderData.get("ok");
    assertThat(okData).isNotNull();
    assertThat(okData.get("dummyId")).isEqualTo("order1_dummy_1");

    assertThat(orderData.containsKey("bad")).isTrue();
    assertThat(orderData.get("bad")).isNull();
    assertThat(executionResult.getErrors().isEmpty()).isFalse();
  }

  @Test
  void argLessListIdFetcherAliases_whenIdFetcherFails_surfacesSameErrorForAllAliases() {
    // `noArgDummies` is an arg-less list @idFetcher field (GetDummyIds). With no arguments,
    // the id-fetcher is called exactly once for the whole field (no per-alias fanout is
    // possible) - so if it fails, every alias of `noArgDummies` must see the same null + error,
    // not silently disappear.
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
                        order(id: "orderBadDummies") {
                          a1: noArgDummies {
                            dummyId
                          }
                          a2: noArgDummies {
                            dummyId
                          }
                        }
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    ExecutionResult executionResult = result.join();
    Map<String, Object> queryData = requireNonNull(executionResult.getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> orderData = requireNonNull((Map<String, Object>) queryData.get("order"));

    assertThat(orderData.containsKey("a1")).isTrue();
    assertThat(orderData.get("a1")).isNull();
    assertThat(orderData.containsKey("a2")).isTrue();
    assertThat(orderData.get("a2")).isNull();
    assertThat(executionResult.getErrors().isEmpty()).isFalse();
  }

  @Test
  void graphqlQueryWithNamedFragment_noAliases_succeeds() {
    // A named fragment spread on `Order`, with no aliases anywhere in the query.
    CompletableFuture<ExecutionResult> result;
    try (VajramKryonExecutor executor = createExecutor()) {
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  VajramExecutionConfig.builder().build(),
                  new GraphQLQuery(
                      """
                      query {\
                        order(id: "order1") {\
                          ...orderFields\
                        }
                      }
                      fragment orderFields on Order {
                        state
                        orderItemNames
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

  @Test
  void graphqlQueryWithNamedFragment_underQueryLevelAliases_succeeds() {
    // The same named fragment is spread under two differently-aliased `order` selections. Each
    // alias must resolve the fragment's fields against its own argument-specific order.
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
                        o1: order(id: "order1") {
                          ...orderFields
                        }
                        o2: order(id: "order2") {
                          ...orderFields
                        }
                      }
                      fragment orderFields on Order {
                        state
                        orderItemNames
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    Map<String, Object> queryData = requireNonNull(result.join().getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> o1Data = requireNonNull((Map<String, Object>) queryData.get("o1"));
    @SuppressWarnings("unchecked")
    Map<String, Object> o2Data = requireNonNull((Map<String, Object>) queryData.get("o2"));

    assertThat(o1Data.get("state")).isEqualTo(State.COMPLETED);
    assertThat(o1Data.get("orderItemNames")).isEqualTo(List.of("order1_1", "order1_2"));
    assertThat(o2Data.get("state")).isEqualTo(State.COMPLETED);
    assertThat(o2Data.get("orderItemNames")).isEqualTo(List.of("order2_1", "order2_2"));
  }

  @Test
  void graphqlQueryWithNamedFragment_havingFieldAliasesInsideFragment_succeeds() {
    // Fields aliased *inside* the fragment definition itself (not at the spread site) must
    // resolve under their aliased response keys.
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
                          ...orderFields
                        }
                      }
                      fragment orderFields on Order {
                        s: state
                        names: orderItemNames
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    Map<String, Object> queryData = requireNonNull(result.join().getData());
    @SuppressWarnings("unchecked")
    Map<String, Object> orderData = requireNonNull((Map<String, Object>) queryData.get("order"));

    assertThat(orderData.get("s")).isEqualTo(State.COMPLETED);
    assertThat(orderData.get("names")).isEqualTo(List.of("order1_1", "order1_2"));
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

  private VajramKryonExecutor createExecutor() {
    return kGraph
        .build()
        .createExecutor(KrystalExecutorConfig.builder().executorService(executorLease.get()));
  }
}

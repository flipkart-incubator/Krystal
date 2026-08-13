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

  private VajramKryonExecutor createExecutor() {
    return kGraph
        .build()
        .createExecutor(KrystalExecutorConfig.builder().executorService(executorLease.get()));
  }
}

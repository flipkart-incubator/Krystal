package com.flipkart.krystal.vajram.graphql.samples;

import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.dispatchTrait;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.vajram.graphql.api.execution.GraphQLQuery;
import com.flipkart.krystal.vajram.graphql.api.execution.GraphQlExecutionFacade;
import com.flipkart.krystal.vajram.graphql.api.schema.GraphQlLoader;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate_Req;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy;
import com.flipkart.krystal.vajram.graphql.samples.order.Order;
import com.flipkart.krystal.vajram.graphql.samples.querytype.QueryType;
import com.flipkart.krystal.vajram.graphql.samples.querytype.QueryType_GQlAggr_Req;
import com.flipkart.krystal.vajram.json.Json;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
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

public class GraphQlSamplesE2ETest {

  private static final Duration TEST_TIMEOUT = Duration.ofSeconds(1000);

  private static SingleThreadExecutorsPool EXEC_POOL;
  private static GraphQL GRAPHQL;
  private KrystexGraphBuilder kGraph;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL =
        new SingleThreadExecutorsPool(
            "GraphQlSamplesE2ETest", Runtime.getRuntime().availableProcessors());
    GraphQlLoader graphQlLoader = new GraphQlLoader();
    GRAPHQL = graphQlLoader.getGraphQl();
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
            dispatchTrait(GraphQlOperationAggregate_Req.class, vGraph)
                .alwaysTo(QueryType_GQlAggr_Req.class)));
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void graphqlQueryExecution_succeeds() throws JsonProcessingException {
    CompletableFuture<ExecutionResult> result;
    try (KrystexVajramExecutor executor = createExecutor()) {
      result =
          new GraphQlExecutionFacade(GRAPHQL)
              .executeGraphQl(
                  executor,
                  KryonExecutionConfig.builder(),
                  new GraphQLQuery(
                      """
                      query {
                        order(id: "order1") {
                          orderItemNames
                          state
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
    Object data = executionResult.getData();
    assertThat(data).isInstanceOf(QueryType.class);
    QueryType queryType = (QueryType) data;

    Order order = requireNonNull(queryType.order());
    Dummy dummy = requireNonNull(queryType.dummy());
    Order mostRecentOrder = requireNonNull(queryType.mostRecentOrder());
    assertThat(order.orderItemNames()).isEqualTo(List.of("order1_1", "order1_2"));
    assertThat(order.nameString()).isEqualTo("testOrderName");
    assertThat(order.__typename()).isEqualTo("Order");
    assertThat(dummy.__typename()).isEqualTo("Dummy");
    assertThat(mostRecentOrder.orderItemNames())
        .isEqualTo(List.of("MostRecentOrderOf_user1_1", "MostRecentOrderOf_user1_2"));

    System.out.println(
        Json.OBJECT_WRITER
            .withDefaultPrettyPrinter()
            .writeValueAsString(executionResult.toSpecification()));
  }

  private KrystexVajramExecutor createExecutor() {
    return kGraph
        .build()
        .createExecutor(
            KrystexVajramExecutorConfig.builder()
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder().executorService(executorLease.get()))
                .build());
  }
}

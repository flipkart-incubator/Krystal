package com.flipkart.krystal.vajram.graphql.samples;

import static com.flipkart.krystal.vajramexecutor.krystex.traits.PredicateDispatchUtil.dispatchTrait;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.graphql.api.execution.GraphQLQuery;
import com.flipkart.krystal.vajram.graphql.api.execution.GraphQlExecutionFacade;
import com.flipkart.krystal.vajram.graphql.api.schema.GraphQlLoader;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate_Req;
import com.flipkart.krystal.vajram.graphql.samples.querytype.QueryType_GQlAggr_Req;
import com.flipkart.krystal.vajram.json.Json;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.time.Duration;
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
  private VajramKryonGraph graph;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL =
        new SingleThreadExecutorsPool(
            "GraphQlSamplesE2ETest", Runtime.getRuntime().availableProcessors());
    GraphQlLoader graphQlLoader = new GraphQlLoader();
    GRAPHQL = graphQlLoader.loadGraphQl(graphQlLoader.getTypeDefinitionRegistry());
  }

  @AfterAll
  static void afterAll() {
    EXEC_POOL.close();
  }

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    this.graph =
        VajramKryonGraph.builder()
            .loadFromPackage(this.getClass().getPackage().getName())
            .loadFromPackage(GraphQlOperationAggregate.class.getPackageName())
            .build();
    graph.registerTraitDispatchPolicies(
        dispatchTrait(GraphQlOperationAggregate_Req.class, graph)
            .alwaysTo(QueryType_GQlAggr_Req.class));
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
                          __typename
                        }
                        dummy(dummyId: "dummy1") {
                          name
                          age
                          f1
                          __typename
                        }
                        __typename
                      }
                      """,
                      Map.of()));
    }
    assertThat(result).succeedsWithin(TEST_TIMEOUT);
    System.out.println(
        Json.OBJECT_WRITER
            .withDefaultPrettyPrinter()
            .writeValueAsString(result.join().toSpecification()));
  }

  private KrystexVajramExecutor createExecutor() {
    return graph.createExecutor(
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder().executorService(executorLease.get()))
            .build());
  }
}

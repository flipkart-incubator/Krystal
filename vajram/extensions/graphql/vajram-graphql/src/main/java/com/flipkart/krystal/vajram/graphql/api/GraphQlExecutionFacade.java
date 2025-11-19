package com.flipkart.krystal.vajram.graphql.api;

import static com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy.GRAPHQL_OPERATION_REQUEST_CTX_KEY;
import static com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy.VAJRAM_INVOCATION_CTX_KEY;

import com.flipkart.krystal.core.VajramInvocation;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOpTypeModel;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlQueryAggregate_ReqImmutPojo;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutionId;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public class GraphQlExecutionFacade {

  private final GraphQL graphQL;

  @Inject
  public GraphQlExecutionFacade(GraphQL graphQL) {
    this.graphQL = graphQL;
  }

  public CompletableFuture<ExecutionResult> executeGraphQlRequest(
      KrystexVajramExecutor krystexVajramExecutor,
      KryonExecutionConfig kryonExecutionConfig,
      GraphQLQuery query,
      String taskId) {
    Map<Object, Object> graphQLContext = new HashMap<>();

    graphQLContext.put(
        GRAPHQL_OPERATION_REQUEST_CTX_KEY,
        switch (query.operation()) {
          case QUERY -> GraphQlQueryAggregate_ReqImmutPojo._builder();
          default ->
              throw new UnsupportedOperationException(
                  query.operation() + " not supported by framework");
        });
    graphQLContext.put(
        VAJRAM_INVOCATION_CTX_KEY,
        (VajramInvocation<GraphQlOpTypeModel>)
            requestResponseFuture ->
                krystexVajramExecutor.execute(requestResponseFuture, kryonExecutionConfig));

    ExecutionInput executionInput =
        ExecutionInput.newExecutionInput()
            .graphQLContext(graphQLContext)
            .query(query.query())
            .variables(query.variables())
            .executionId(ExecutionId.from(taskId))
            .build();
    return graphQL.executeAsync(executionInput);
  }
}

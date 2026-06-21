package com.flipkart.krystal.vajram.graphql.api.execution;

import static com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy.VAJRAM_INVOCATION_CTX_KEY;
import static com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject._asExecutionResult;

import com.flipkart.krystal.core.VajramInvocation;
import com.flipkart.krystal.krystex.kryon.VajramExecutionConfig;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutionId;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public class GraphQlExecutionFacade {

  private final GraphQL graphQL;

  @Inject
  public GraphQlExecutionFacade(GraphQL graphQL) {
    this.graphQL = graphQL;
  }

  public CompletableFuture<ExecutionResult> executeGraphQl(
      VajramKryonExecutor vajramKryonExecutor,
      VajramExecutionConfig vajramExecutionConfig,
      GraphQLQuery query) {

    // TODO: vajramExecutionConfig.disabledDependentChains();

    ExecutionInput executionInput =
        ExecutionInput.newExecutionInput()
            .graphQLContext(
                Map.of(
                    VAJRAM_INVOCATION_CTX_KEY,
                    (VajramInvocation<GraphQlOperationObject>)
                        requestResponseFuture ->
                            vajramKryonExecutor.execute(
                                requestResponseFuture, vajramExecutionConfig)))
            .query(query.query())
            .variables(query.variables())
            .executionId(ExecutionId.from(vajramExecutionConfig.executionId()))
            .build();

    return graphQL
        .executeAsync(executionInput)
        .thenApply(
            executionResult -> {
              if (executionResult.getData() instanceof GraphQlObjectResult graphQlObjectResult) {
                //noinspection unchecked
                return _asExecutionResult(graphQlObjectResult.graphQlOperationObject());
              } else {
                return executionResult;
              }
            });
  }
}

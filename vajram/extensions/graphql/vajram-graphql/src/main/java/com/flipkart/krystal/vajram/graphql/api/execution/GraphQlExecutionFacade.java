package com.flipkart.krystal.vajram.graphql.api.execution;

import static com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy.VAJRAM_INVOCATION_CTX_KEY;
import static com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject._asExecutionResult;

import com.flipkart.krystal.core.VajramInvocation;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig.KryonExecutionConfigBuilder;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
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
      KrystexVajramExecutor krystexVajramExecutor,
      KryonExecutionConfigBuilder kryonExecutionConfigBuilder,
      GraphQLQuery query) {

    // TODO: kryonExecutionConfig.disabledDependentChains();
    KryonExecutionConfig kryonExecutionConfig = kryonExecutionConfigBuilder.build();

    ExecutionInput executionInput =
        ExecutionInput.newExecutionInput()
            .graphQLContext(
                Map.of(
                    VAJRAM_INVOCATION_CTX_KEY,
                    (VajramInvocation<GraphQlOperationObject>)
                        requestResponseFuture ->
                            krystexVajramExecutor.execute(
                                requestResponseFuture, kryonExecutionConfig)))
            .query(query.query())
            .variables(query.variables())
            .executionId(ExecutionId.from(kryonExecutionConfig.executionId()))
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

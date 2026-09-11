package com.flipkart.krystal.vajram.graphql.api.model;

import static com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector.defaultCollector;
import static graphql.ErrorType.DataFetchingException;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import graphql.ExecutionResult;
import graphql.ExecutionResult.Builder;
import graphql.GraphQLError;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface GraphQlOperation extends GraphQlObject
    permits GraphQlOperationImpl, GraphQlOperationError {

  default @Nullable Map<Object, Object> graphql_extensions() {
    return null;
  }

  @Nullable
  default List<GraphQLError> graphql_errors(ErrorCollector errorCollector) {
    _collectErrors(errorCollector, new ArrayList<>());
    return errorCollector.getErrors();
  }

  static ExecutionResult _asExecutionResult(Errable<GraphQlOperation> errable) {
    return errable.mapToValue(
        /* ifFailure= */ failure -> {
          Throwable error = failure.error();
          if (error instanceof GraphQLError graphQLError) {
            return ExecutionResult.newExecutionResult().addError(graphQLError).build();
          }
          return ExecutionResult.newExecutionResult()
              .addError(
                  GraphQLError.newError()
                      .message("Error encountered while computing GraphQl execution result.")
                      .errorType(DataFetchingException)
                      .build())
              .build();
        },
        /* ifNil= */ () -> ExecutionResult.newExecutionResult().build(),
        /* ifNonNil= */ GraphQlOperation::_asExecutionResult);
  }

  default ExecutionResult _asExecutionResult() {
    if (this instanceof GraphQlOperationError operationError) {
      return operationError.executionResult();
    }
    Builder<?> builder = ExecutionResult.newExecutionResult().data(graphql_data());

    List<GraphQLError> errors = graphql_errors(defaultCollector());
    if (errors != null) {
      builder.errors(errors);
    }

    Map<Object, Object> extensions = graphql_extensions();
    if (extensions != null) {
      builder.extensions(extensions);
    }

    return builder.build();
  }
}

package com.flipkart.krystal.vajram.graphql.api.model;

import static com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector.defaultCollector;
import static graphql.ErrorType.DataFetchingException;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface GraphQlOperationObject extends GraphQlObject
    permits GraphQlOperationObjectMap, GraphQlOperationError {

  default @Nullable Map<Object, Object> graphql_extensions() {
    return null;
  }

  @Nullable
  default List<GraphQLError> graphql_errors(ErrorCollector errorCollector) {
    _collectErrors(errorCollector, new ArrayList<>());
    return errorCollector.getErrors();
  }

  static ExecutionResult _asExecutionResult(Errable<GraphQlOperationObject> errable) {
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
        /* ifNonNil= */ GraphQlOperationObject::_asExecutionResult);
  }

  default ExecutionResult _asExecutionResult() {
    if (this instanceof GraphQlOperationError operationError) {
      return operationError.executionResult();
    }
    return ExecutionResult.newExecutionResult()
        .data(graphql_data())
        .errors(graphql_errors(defaultCollector()))
        .extensions(graphql_extensions())
        .build();
  }
}

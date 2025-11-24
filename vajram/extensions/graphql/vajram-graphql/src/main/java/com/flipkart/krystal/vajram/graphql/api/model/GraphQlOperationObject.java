package com.flipkart.krystal.vajram.graphql.api.model;

import static com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector.defaultCollector;
import static graphql.ErrorType.DataFetchingException;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.ModelClusterRoot;
import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import com.flipkart.krystal.vajram.json.Json;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelClusterRoot(
    immutableRoot = GraphQlOperationObject_Immut.class,
    builderRoot = GraphQlOperationObject_Immut.Builder.class)
public interface GraphQlOperationObject extends GraphQlObject {

  @Nullable Map<Object, Object> _extensions();

  @Override
  GraphQlOperationObject_Immut _build();

  @Override
  GraphQlOperationObject_Immut.Builder _asBuilder();

  @Nullable
  default List<GraphQLError> _errors() {
    ErrorCollector errorCollector = defaultCollector();
    _collectErrors(errorCollector, new ArrayList<>());
    return errorCollector.getErrors();
  }

  static ExecutionResult _asExecutionResult(Errable<GraphQlOperationObject> errable) {
    return errable.map(
        failure -> {
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
        () -> ExecutionResult.newExecutionResult().build(),
        nonNil -> nonNil.value()._asExecutionResult());
  }

  default ExecutionResult _asExecutionResult() {
    if (this instanceof GraphQlOperationError operationError) {
      return operationError.executionResult();
    }
    return ExecutionResult.newExecutionResult()
        .data(this._build())
        .errors(_errors())
        .extensions(_extensions())
        .build();
  }

  // TODO: Delete
  default byte[] _serialize() throws Exception {
    Map<String, @Nullable Object> map = new HashMap<>(3);
    map.put("data", this);
    map.put("errors", _errors());
    map.put("extensions", _extensions());
    return Json.OBJECT_WRITER.writeValueAsBytes(map);
  }
}

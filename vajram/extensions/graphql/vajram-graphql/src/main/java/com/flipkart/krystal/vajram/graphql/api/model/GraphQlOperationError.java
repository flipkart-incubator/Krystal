package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import graphql.ExecutionResult;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

public record GraphQlOperationError(ExecutionResult executionResult)
    implements GraphQlOperationObject {

  public static @PolyNull GraphQlOperationObject from(@PolyNull ExecutionResult executionResult) {
    if (executionResult == null) {
      return null;
    }
    return new GraphQlOperationError(executionResult);
  }

  @Override
  public void _collectErrors(ErrorCollector errorCollector, List<Object> path) {
    executionResult.getErrors().forEach(errorCollector::addError);
  }

  @Override
  public @Nullable Map<Object, Object> graphql_extensions() {
    return executionResult.getExtensions();
  }

  @Override
  public Map<String, Object> graphql_data() {
    return Map.of();
  }
}

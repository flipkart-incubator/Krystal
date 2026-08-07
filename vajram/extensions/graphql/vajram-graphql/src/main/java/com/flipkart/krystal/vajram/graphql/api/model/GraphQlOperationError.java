package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import graphql.ExecutionResult;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public record GraphQlOperationError(ExecutionResult executionResult)
    implements GraphQlOperationObject {

  public static @Nullable GraphQlOperationObject from(@Nullable ExecutionResult executionResult) {
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
  public @Nullable String graphql_typename() {
    return null;
  }

  @Override
  public ExecutionContext graphql_executionContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public VajramExecutionStrategy graphql_executionStrategy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExecutionStrategyParameters graphql_executionStrategyParams() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> graphql_data() {
    return Map.of();
  }
}

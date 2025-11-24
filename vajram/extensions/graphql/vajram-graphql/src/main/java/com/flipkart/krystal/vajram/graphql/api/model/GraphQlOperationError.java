package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject_Immut.Builder;
import graphql.ExecutionResult;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public record GraphQlOperationError(ExecutionResult executionResult)
    implements GraphQlOperationObject_Immut {

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
  public GraphQlOperationObject_Immut _build() {
    return this;
  }

  @Override
  public @Nullable Map<Object, Object> _extensions() {
    return executionResult.getExtensions();
  }

  @Override
  public GraphQlOperationError _newCopy() {
    return this;
  }

  @Override
  public Builder _asBuilder() {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable String __typename() {
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
}

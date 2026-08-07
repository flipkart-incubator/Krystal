package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface GraphQlObject permits GraphQlOperationObject, GraphQlObjectMap {
  /**
   * Returns the __typename of a graphql type according to the graphql spec or null if it has yet
   * been queried..
   */
  default @Nullable String graphql_typename() {
    return null;
  }

  /**
   * Returns the {@link ExecutionContext} which for the execution in which this object was created.
   */
  ExecutionContext graphql_executionContext();

  VajramExecutionStrategy graphql_executionStrategy();

  /**
   * Returns the {@link ExecutionStrategyParameters} for the execution in which this object was
   * created.
   */
  ExecutionStrategyParameters graphql_executionStrategyParams();

  /**
   * Collects errors from this model and all nested models using the visitor pattern.
   *
   * @param errorCollector The collector to accumulate errors
   * @param path The current path in the GraphQL response tree
   */
  default void _collectErrors(ErrorCollector errorCollector, List<Object> path) {}

  Map<String, Object> graphql_data();
}

package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelClusterRoot;
import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@ModelClusterRoot(
    immutableRoot = GraphQlObject_Immut.class,
    builderRoot = GraphQlObject_Immut.Builder.class)
public interface GraphQlObject extends Model {
  /**
   * Returns the __typename of a graphql type according to the graphql spec or null if it has yet
   * been queried..
   */
  @Nullable String __typename();

  ExecutionContext graphql_executionContext();

  VajramExecutionStrategy graphql_executionStrategy();

  ExecutionStrategyParameters graphql_executionStrategyParams();

  /**
   * Collects errors from this model and all nested models using the visitor pattern.
   *
   * @param errorCollector The collector to accumulate errors
   * @param path The current path in the GraphQL response tree
   */
  void _collectErrors(ErrorCollector errorCollector, List<Object> path);
}

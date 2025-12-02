package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;

public interface GraphQlOperationObject_Immut extends GraphQlObject_Immut, GraphQlOperationObject {
  @Override
  GraphQlOperationObject_Immut _build();

  interface Builder extends GraphQlObject_Immut.Builder {

    @Override
    Builder graphql_executionContext(ExecutionContext graphql_executionContext);

    @Override
    Builder graphql_executionStrategy(VajramExecutionStrategy graphql_executionStrategy);

    @Override
    Builder graphql_executionStrategyParams(
        ExecutionStrategyParameters graphql_executionStrategyParams);

    @Override
    GraphQlOperationObject_Immut _build();
  }
}

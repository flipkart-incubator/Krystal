package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;

public interface GraphQlTypeModel_Immut extends GraphQlTypeModel, ImmutableModel {
  interface Builder extends ImmutableModel.Builder {

    Builder graphql_executionContext(ExecutionContext graphql_executionContext);

    Builder graphql_executionStrategy(VajramExecutionStrategy graphql_executionStrategy);

    Builder graphql_executionStrategyParams(
        ExecutionStrategyParameters graphql_executionStrategyParams);
  }
}

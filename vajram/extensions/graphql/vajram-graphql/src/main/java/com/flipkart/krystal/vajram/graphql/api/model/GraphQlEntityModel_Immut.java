package com.flipkart.krystal.vajram.graphql.api.model;

public interface GraphQlEntityModel_Immut<I extends GraphQlEntityId>
    extends GraphQlEntityModel<I>, GraphQlTypeModel_Immut {
  interface Builder<I extends GraphQlEntityId> extends GraphQlTypeModel_Immut.Builder {
    I id();

    //    Builder graphql_executionContext(ExecutionContext graphql_executionContext);
    //
    //    Builder graphql_executionStrategy(VajramExecutionStrategy graphql_executionStrategy);
    //
    //    Builder graphql_executionStrategyParams(
    //        ExecutionStrategyParameters graphql_executionStrategyParams);
  }
}

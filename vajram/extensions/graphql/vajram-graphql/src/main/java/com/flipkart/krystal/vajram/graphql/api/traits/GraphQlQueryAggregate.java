package com.flipkart.krystal.vajram.graphql.api.traits;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOpTypeModel;
import graphql.ExecutionInput;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;

@CallGraphDelegationMode(SYNC)
@Trait
public interface GraphQlQueryAggregate<T extends GraphQlOpTypeModel> extends TraitDef<T> {
  class _Inputs {
    @IfAbsent(FAIL)
    ExecutionContext graphql_executionContext;

    @IfAbsent(FAIL)
    VajramExecutionStrategy graphql_executionStrategy;

    @IfAbsent(FAIL)
    ExecutionStrategyParameters graphql_executionStrategyParams;
  }
}

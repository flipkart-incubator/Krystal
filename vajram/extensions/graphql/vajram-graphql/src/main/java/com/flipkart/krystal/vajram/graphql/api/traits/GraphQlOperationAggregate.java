package com.flipkart.krystal.vajram.graphql.api.traits;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import graphql.ExecutionInput;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.language.OperationDefinition.Operation;

@CallGraphDelegationMode(SYNC)
@Trait
public interface GraphQlOperationAggregate<T extends GraphQlOperationObject> extends TraitDef<T> {
  class _Inputs {
    @IfAbsent(FAIL)
    ExecutionInput executionInput;

    @IfAbsent(FAIL)
    Operation operationType;

    @IfAbsent(FAIL)
    ExecutionContext graphql_executionContext;

    @IfAbsent(FAIL)
    VajramExecutionStrategy graphql_executionStrategy;

    @IfAbsent(FAIL)
    ExecutionStrategyParameters graphql_executionStrategyParams;
  }
}

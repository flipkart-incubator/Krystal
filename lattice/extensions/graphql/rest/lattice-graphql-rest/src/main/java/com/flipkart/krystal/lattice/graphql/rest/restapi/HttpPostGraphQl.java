package com.flipkart.krystal.lattice.graphql.rest.restapi;

import static com.flipkart.krystal.lattice.graphql.rest.restapi.HttpPostGraphQl_Fac.queryResponse_n;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject._asExecutionResult;
import static java.util.Objects.requireNonNullElse;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.lattice.ext.rest.api.methods.POST;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate_Req;
import graphql.ExecutionInput;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

@InvocableOutsideGraph
@InvocableOutsideProcess
@POST
@Vajram
public abstract class HttpPostGraphQl extends ComputeVajramDef<Response> {
  static class _Inputs {
    @IfAbsent(FAIL)
    String query;

    Map<String, Object> variables;

    String operationName;

    Map<String, Object> extensions;
  }

  static class _InternalFacets {
    @Dependency(onVajram = GraphQlOperationAggregate.class)
    @Nullable GraphQlOperationObject queryResponse;
  }

  @Resolve(dep = queryResponse_n, depInputs = GraphQlOperationAggregate_Req.executionInput_n)
  static ExecutionInput computeExecutionInput(
      String query,
      @Nullable Map<String, Object> variables,
      @Nullable Map<String, Object> extensions,
      @Nullable String operationName) {
    return ExecutionInput.newExecutionInput()
        .query(query)
        .operationName(operationName)
        .variables(requireNonNullElse(variables, Map.of()))
        .extensions(requireNonNullElse(extensions, Map.of()))
        .build();
  }

  @Output
  static Response output(Errable<GraphQlOperationObject> queryResponse) {
    return Response.ok(_asExecutionResult(queryResponse)).build();
  }
}

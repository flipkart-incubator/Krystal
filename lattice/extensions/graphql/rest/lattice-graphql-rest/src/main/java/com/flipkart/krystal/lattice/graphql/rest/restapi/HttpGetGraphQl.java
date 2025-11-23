package com.flipkart.krystal.lattice.graphql.rest.restapi;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject._asExecutionResult;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.lattice.ext.rest.api.Body;
import com.flipkart.krystal.lattice.ext.rest.api.methods.GET;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.checkerframework.checker.nullness.qual.Nullable;

@InvocableOutsideGraph
@InvocableOutsideProcess
@GET
@Vajram
public abstract class HttpGetGraphQl extends ComputeVajramDef<Response> {
  static class _Inputs {
    @Body ByteArray body;
  }

  static class _InternalFacets {
    @Inject
    @IfAbsent(FAIL)
    UriInfo uriInfo;

    @Dependency(onVajram = GraphQlOperationAggregate.class)
    @Nullable GraphQlOperationObject queryResponse;
  }

  @Output
  static Response output(Errable<GraphQlOperationObject> queryResponse) {
    return Response.ok(_asExecutionResult(queryResponse)).build();
  }
}

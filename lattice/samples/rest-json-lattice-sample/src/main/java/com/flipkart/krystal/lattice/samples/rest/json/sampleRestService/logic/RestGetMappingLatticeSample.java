package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.lattice.ext.rest.api.Path;
import com.flipkart.krystal.lattice.ext.rest.api.PathParam;
import com.flipkart.krystal.lattice.ext.rest.api.QueryParam;
import com.flipkart.krystal.lattice.ext.rest.api.methods.GET;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.models.JsonResponse;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.models.JsonResponse_Immut;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A sample vajram to demonstrate integration between rest and vajrams. This vajram can be invoked
 * via an Http Rest call by remote clients.
 */
@SuppressWarnings("initialization.field.uninitialized")
@InvocableOutsideGraph
@InvocableOutsideProcess
@Path("{fullPath: .*}")
@GET
@Vajram
public abstract class RestGetMappingLatticeSample extends ComputeVajramDef<JsonResponse> {
  static class _Inputs {
    @IfAbsent(FAIL)
    @PathParam
    String fullPath;

    @QueryParam String name;

    @QueryParam String age;
  }

  static class _InternalFacets {
    @Inject
    @IfAbsent(FAIL)
    JsonResponse_Immut.Builder responseBuilder;

    @Inject
    @IfAbsent(FAIL)
    UriInfo uriInfo;
  }

  @Output
  static JsonResponse output(
      String fullPath,
      @Nullable String name,
      @Nullable String age,
      UriInfo uriInfo,
      JsonResponse_Immut.Builder responseBuilder) {
    return responseBuilder
        .path(fullPath)
        .qp_name(name)
        .qp_age(age)
        .uriInfo(uriInfo.getPath())
        .string(uriInfo.getQueryParameters().toString())
        .mandatoryInt(1)
        ._build();
  }
}

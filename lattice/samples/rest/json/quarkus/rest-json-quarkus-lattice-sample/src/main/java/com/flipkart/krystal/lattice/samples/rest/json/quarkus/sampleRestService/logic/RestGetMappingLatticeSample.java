package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.util.Objects.requireNonNullElseGet;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.annos.InvocableOutsideProcess;
import com.flipkart.krystal.lattice.core.di.ByContentType;
import com.flipkart.krystal.lattice.ext.rest.api.Path;
import com.flipkart.krystal.lattice.ext.rest.api.methods.GET;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse_Immut;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse_ImmutJson;
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
  interface _Inputs extends RestGetMappingLatticeSample_Inputs {}

  interface _InternalFacets {
    @Inject
    @ByContentType
    JsonResponse_Immut.Builder responseBuilder();

    @Inject
    @IfAbsent(FAIL)
    UriInfo uriInfo();
  }

  @Output
  static JsonResponse output(
      String fullPath,
      @Nullable String name,
      @Nullable String age,
      UriInfo uriInfo,
      JsonResponse_Immut.@Nullable Builder responseBuilder) {
    return requireNonNullElseGet(responseBuilder, JsonResponse_ImmutJson::_builder)
        .path(fullPath)
        .qp_name(name)
        .qp_age(age)
        .uriInfo(uriInfo.getPath())
        .string(uriInfo.getQueryParameters().toString())
        .mandatoryInt(1)
        ._build();
  }
}

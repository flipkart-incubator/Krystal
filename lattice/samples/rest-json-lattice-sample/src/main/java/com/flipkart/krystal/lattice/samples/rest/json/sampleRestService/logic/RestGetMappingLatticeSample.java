package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.lattice.rest.api.Path;
import com.flipkart.krystal.lattice.rest.api.PathParam;
import com.flipkart.krystal.lattice.rest.api.QueryParam;
import com.flipkart.krystal.lattice.rest.api.methods.GET;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.models.JsonResponse;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.models.JsonResponse_Immut;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;
import java.util.Optional;

/**
 * A sample vajram to demonstrate integration between rest and vajrams. This vajram can be invoked
 * via a Http Rest call by remote clients.
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
    JsonResponse_Immut.Builder reponseBuilder;

    @Inject
    @IfAbsent(FAIL)
    UriInfo uriInfo;
  }

  @Output
  static JsonResponse output(
      String fullPath,
      Optional<String> name,
      Optional<String> age,
      UriInfo uriInfo,
      JsonResponse_Immut.Builder reponseBuilder) {
    return reponseBuilder
        .path(fullPath)
        .qp_name(name.orElse(null))
        .qp_age(age.orElse(null))
        .uriInfo(uriInfo.getPath())
        .string(uriInfo.getQueryParameters().toString())
        .mandatoryInt(1)
        ._build();
  }
}

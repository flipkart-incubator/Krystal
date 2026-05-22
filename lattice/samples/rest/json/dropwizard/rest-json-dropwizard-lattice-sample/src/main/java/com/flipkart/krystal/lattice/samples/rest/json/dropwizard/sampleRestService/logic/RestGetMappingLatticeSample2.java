package com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.annos.InvocableOutsideProcess;
import com.flipkart.krystal.lattice.core.di.ByContentType;
import com.flipkart.krystal.lattice.ext.rest.api.Path;
import com.flipkart.krystal.lattice.ext.rest.api.methods.GET;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.models.JsonResponse2;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.models.JsonResponse2_Immut;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;

/**
 * A sample vajram to demonstrate integration between rest and vajrams. This vajram can be invoked
 * via an Http Rest call by remote clients.
 */
@SuppressWarnings("initialization.field.uninitialized")
@InvocableOutsideGraph
@InvocableOutsideProcess
@Path("/no_default_serde_protocol_for_response")
@GET
@Vajram
public abstract class RestGetMappingLatticeSample2 extends ComputeVajramDef<JsonResponse2> {
  interface _Inputs {}

  static class _InternalFacets {
    @Inject
    @ByContentType
    @IfAbsent(FAIL)
    JsonResponse2_Immut.Builder responseBuilder;

    @Inject
    @IfAbsent(FAIL)
    UriInfo uriInfo;
  }

  @Output
  static JsonResponse2 output(UriInfo uriInfo, JsonResponse2_Immut.Builder responseBuilder) {
    return responseBuilder.responseValue("success " + uriInfo)._build();
  }
}

package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.annos.InvocableOutsideProcess;
import com.flipkart.krystal.lattice.ext.rest.api.Body;
import com.flipkart.krystal.lattice.ext.rest.api.Path;
import com.flipkart.krystal.lattice.ext.rest.api.PathParam;
import com.flipkart.krystal.lattice.ext.rest.api.methods.POST;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonRequest;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse_Immut;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import jakarta.inject.Inject;

/**
 * A sample vajram to demonstrate integration between rest and vajrams. This vajram can be invoked
 * via a Http Rest call by remote clients.
 */
@SuppressWarnings("initialization.field.uninitialized")
@InvocableOutsideGraph
@InvocableOutsideProcess
@Path("/complex/{context}/path/{name}/{threePaths:[^_]+_[^_]+_[^_]+}/{id}")
@POST
@Vajram
public abstract class RestPostComplexPathMatching extends ComputeVajramDef<JsonResponse> {
  interface _Inputs {
    @IfAbsent(FAIL)
    @Body
    JsonRequest jsonRequest();

    @IfAbsent(FAIL)
    @PathParam
    String context();

    @IfAbsent(FAIL)
    @PathParam
    String name();

    @IfAbsent(FAIL)
    @PathParam
    String threePaths();

    @IfAbsent(FAIL)
    @PathParam
    int id();
  }

  interface _InternalFacets {
    @Inject
    @IfAbsent(FAIL)
    JsonResponse_Immut.Builder responseBuilder();
  }

  @Output
  static JsonResponse output(
      JsonRequest jsonRequest,
      String context,
      String name,
      String threePaths,
      int id,
      JsonResponse_Immut.Builder responseBuilder) {
    return responseBuilder
        .string(
            """
              $$ context: %s $$
              $$ name: %s $$
              $$ threePaths: %s $$
              $$ id: %s $$
              $$ optionalInput: %s $$
              $$ mandatoryInput: %s $$
              $$ conditionallyMandatoryInput: %s $$
              $$ inputWithDefaultValue: %s $$
              $$ optionalLongInput: %s $$
              $$ mandatoryLongInput: %s $$
              $$ optionalByteString: %s $$
              $$ defaultByteString: %s $$
              """
                .formatted(
                    context,
                    name,
                    threePaths,
                    id,
                    jsonRequest.optionalInput(),
                    jsonRequest.mandatoryInput(),
                    jsonRequest.conditionallyMandatoryInput(),
                    jsonRequest.inputWithDefaultValue(),
                    jsonRequest.optionalLongInput(),
                    jsonRequest.mandatoryLongInput(),
                    jsonRequest.optionalByteString(),
                    jsonRequest.defaultByteString()))
        .mandatoryInt(1)
        ._build();
  }
}

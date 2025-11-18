package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.lattice.ext.rest.api.Body;
import com.flipkart.krystal.lattice.ext.rest.api.Path;
import com.flipkart.krystal.lattice.ext.rest.api.PathParam;
import com.flipkart.krystal.lattice.ext.rest.api.methods.POST;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.models.JsonRequest;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.models.JsonResponse;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.models.JsonResponse_Immut;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.serial.SerdeConfig;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.json.Json;
import jakarta.inject.Inject;
import java.util.Optional;

/**
 * A sample vajram to demonstrate integration between rest and vajrams. This vajram can be invoked
 * via a Http Rest call by remote clients.
 */
@SuppressWarnings("initialization.field.uninitialized")
@InvocableOutsideGraph
@InvocableOutsideProcess
@Path("{fullPath: .*}")
@POST
@Vajram
public abstract class RestPostMappingLatticeSample extends ComputeVajramDef<JsonResponse> {
  static class _Inputs {
    @IfAbsent(FAIL)
    @Body
    @SerdeConfig(
        protocol = Json.class,
        contentTypes = {"application/json", "application/x-json-test"})
    JsonRequest jsonRequest;

    @IfAbsent(FAIL)
    @PathParam
    String fullPath;
  }

  static class _InternalFacets {
    @Inject
    @IfAbsent(FAIL)
    JsonResponse_Immut.Builder reponseBuilder;
  }

  @Output
  static JsonResponse output(
      JsonRequest jsonRequest, String fullPath, JsonResponse_Immut.Builder reponseBuilder) {
    return reponseBuilder
        .string(
            """
              $$ PATH: %s $$
              $$ optionalInput: %s $$
              $$ mandatoryInput: %s $$
              $$ conditionallyMandatoryInput: %s $$
              $$ inputWithDefaultValue: %s $$
              $$ optionalLongInput: %s $$
              $$ mandatoryLongInput: %s $$
              $$ optionalByteString: %s $$
              $$ defaultByteString: %s ---- $$
              """
                .formatted(
                    fullPath,
                    jsonRequest.optionalInput(),
                    jsonRequest.mandatoryInput(),
                    jsonRequest.conditionallyMandatoryInput(),
                    jsonRequest.inputWithDefaultValue(),
                    jsonRequest.optionalLongInput(),
                    jsonRequest.mandatoryLongInput(),
                    Optional.ofNullable(jsonRequest.optionalByteString())
                        .map(bytes -> bytes.toString(UTF_8)),
                    jsonRequest.defaultByteString().toString(UTF_8)))
        .mandatoryInt(1)
        ._build();
  }
}

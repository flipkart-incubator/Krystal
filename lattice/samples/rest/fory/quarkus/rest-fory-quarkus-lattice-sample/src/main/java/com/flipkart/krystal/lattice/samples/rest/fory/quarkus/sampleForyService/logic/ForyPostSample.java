package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.util.Objects.requireNonNullElseGet;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.annos.InvocableOutsideProcess;
import com.flipkart.krystal.lattice.core.di.ByContentType;
import com.flipkart.krystal.lattice.ext.rest.api.Body;
import com.flipkart.krystal.lattice.ext.rest.api.Path;
import com.flipkart.krystal.lattice.ext.rest.api.PathParam;
import com.flipkart.krystal.lattice.ext.rest.api.methods.POST;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyRequest;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyResponse;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyResponse_Immut;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyResponse_ImmutFory;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.serial.SerdeConfig;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.fory.Fory;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A sample vajram demonstrating a POST endpoint that accepts a Fory-serialized request body. The
 * request is deserialized from binary Fory format and the fields are echoed in the response.
 */
@InvocableOutsideGraph
@InvocableOutsideProcess
@Path("{fullPath: .*}")
@POST
@Vajram
public abstract class ForyPostSample extends ComputeVajramDef<ForyResponse> {
  interface _Inputs {
    @IfAbsent(FAIL)
    @Body
    @SerdeConfig(protocol = Fory.class)
    ForyRequest foryRequest();

    @IfAbsent(FAIL)
    @PathParam
    String fullPath();
  }

  interface _InternalFacets {
    @Inject
    @ByContentType
    ForyResponse_Immut.Builder responseBuilder();
  }

  @Output
  static ForyResponse output(
      ForyRequest foryRequest,
      String fullPath,
      ForyResponse_Immut.@Nullable Builder responseBuilder) {
    return requireNonNullElseGet(responseBuilder, ForyResponse_ImmutFory::_builder)
        .message(
            """
            $$ PATH: %s $$
            $$ mandatoryInput: %s $$
            $$ mandatoryLongInput: %s $$
            $$ optionalInput: %s $$
            $$ optionalLongInput: %s $$
            """
                .formatted(
                    fullPath,
                    foryRequest.mandatoryInput(),
                    foryRequest.mandatoryLongInput(),
                    foryRequest.optionalInput(),
                    foryRequest.optionalLongInput()))
        .mandatoryInt(1)
        ._build();
  }
}

package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static java.util.Objects.requireNonNullElseGet;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.annos.InvocableOutsideProcess;
import com.flipkart.krystal.lattice.core.di.ByContentType;
import com.flipkart.krystal.lattice.ext.rest.api.Path;
import com.flipkart.krystal.lattice.ext.rest.api.PathParam;
import com.flipkart.krystal.lattice.ext.rest.api.QueryParam;
import com.flipkart.krystal.lattice.ext.rest.api.methods.GET;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyResponse;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyResponse_Immut;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyResponse_ImmutFory;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A sample vajram demonstrating Fory-backed models served via a GET REST endpoint. Query parameters
 * and the path are echoed back in the response.
 */
@InvocableOutsideGraph
@InvocableOutsideProcess
@Path("{fullPath: .*}")
@GET
@Vajram
public abstract class ForyGetSample extends ComputeVajramDef<ForyResponse> {
  interface _Inputs {
    @IfAbsent(FAIL)
    @PathParam
    String fullPath();

    @QueryParam
    String name();

    @QueryParam
    String age();
  }

  interface _InternalFacets {
    @Inject
    @ByContentType
    ForyResponse_Immut.Builder responseBuilder();
  }

  @Output
  static ForyResponse output(
      String fullPath,
      @Nullable String name,
      @Nullable String age,
      ForyResponse_Immut.@Nullable Builder responseBuilder) {
    return requireNonNullElseGet(responseBuilder, ForyResponse_ImmutFory::_builder)
        .path(fullPath)
        .queryName(name)
        .queryAge(age)
        .message("GET response via Fory serde")
        .mandatoryInt(1)
        ._build();
  }
}

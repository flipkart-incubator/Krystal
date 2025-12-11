package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.json.Json;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;

@InvocableOutsideGraph
@InvocableOutsideProcess
@SupportedModelProtocols({Json.class, PlainJavaObject.class})
@Vajram
public abstract class RestStreamingSample extends ComputeVajramDef<Response> {
  @Output
  static Response streamBytes() {
    GenericEntity<Multi<byte[]>> entity1 =
        new GenericEntity<>(
            Multi.createBy()
                .repeating()
                .supplier("All work and no play"::getBytes)
                .atMost(1000)) {};
    return Response.ok("All work and no play".getBytes()).build();
  }
}

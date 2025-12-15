package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic;

import static io.vertx.core.buffer.Buffer.buffer;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.json.Json;
import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.Flow.Publisher;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@InvocableOutsideGraph
@InvocableOutsideProcess
@SupportedModelProtocols({Json.class, PlainJavaObject.class})
@Vajram
@Produces(MediaType.TEXT_PLAIN)
public abstract class RestStreamingSample extends ComputeVajramDef<Publisher<Buffer>> {
  private static int count;

  @Output
  static Publisher<Buffer> streamBytes() {
    return Multi.createBy()
        .repeating()
        .supplier(
            () -> {
              long counter = count++;
              log.info("StreamingDirect.post {}", counter);
              return buffer(counter + ": All work and no play\n");
            })
        .atMost(1_000);
  }
}

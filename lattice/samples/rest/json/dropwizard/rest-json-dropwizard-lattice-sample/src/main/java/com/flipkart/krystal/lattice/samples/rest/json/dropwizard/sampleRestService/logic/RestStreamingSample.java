package com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.lattice.vajram.sdk.InvocableOutsideProcess;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.json.Json;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ChunkedOutput;

@Slf4j
@InvocableOutsideGraph
@InvocableOutsideProcess
@SupportedModelProtocols({Json.class, PlainJavaObject.class})
@Vajram
@Produces(MediaType.TEXT_PLAIN)
public abstract class RestStreamingSample extends ComputeVajramDef<ChunkedOutput<String>> {
  private static int count;

  @Output
  static ChunkedOutput<String> streamBytes() {
    ChunkedOutput<String> chunkedOutput = new ChunkedOutput<>(ByteBuffer.class);
    Multi.createBy()
        .repeating()
        .uni(
            () -> {
              long counter = count++;
              log.info("StreamingDirect.post {}", counter);
              return Uni.createFrom()
                  .item(counter + ": All work and no play\n")
                  .onItem()
                  .delayIt()
                  .by(Duration.ofMillis(10));
            })
        .atMost(1_000)
        .subscribe()
        .with(
            string -> {
              try {
                chunkedOutput.write(string);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            () -> {
              try {
                chunkedOutput.close();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    return chunkedOutput;
  }
}

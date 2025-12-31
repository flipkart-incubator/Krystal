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
public abstract class RestStreamingSample extends ComputeVajramDef<ChunkedOutput<ByteBuffer>> {
  private static int count;

  @Output
  static ChunkedOutput<ByteBuffer> streamBytes() {
    ChunkedOutput<ByteBuffer> chunkedOutput = new ChunkedOutput<>(ByteBuffer.class);
    Multi.createBy()
        .repeating()
        .uni(
            () -> {
              long counter = count++;
              log.info("StreamingDirect.post {}", counter);
              return Uni.createFrom()
                  .item(ByteBuffer.wrap((counter + ": All work and no play\n").getBytes()))
                  .onItem()
                  .delayIt()
                  .by(Duration.ofSeconds(1));
            })
        .atMost(1_000)
        .subscribe()
        .with(
            byteBuffer -> {
              try {
                chunkedOutput.write(byteBuffer);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    return chunkedOutput;
  }
}

package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.logic;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.Flow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("StreamingDirect")
public class StreamingDirect {

  int count;

  @POST
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Flow.Publisher<byte[]> post() {
    return Multi.createBy()
        .repeating()
        .supplier(
            () -> {
              long counter = count++;
              log.info("StreamingDirect.post {}", counter);
              return (counter + ": All work and no play\n").getBytes();
            })
        .atMost(1_000_000);
  }
}

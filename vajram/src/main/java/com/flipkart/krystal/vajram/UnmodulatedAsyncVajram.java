package com.flipkart.krystal.vajram;

import java.util.concurrent.CompletableFuture;

public non-sealed abstract class UnmodulatedAsyncVajram<Request, EnrichedRequest, Response>
    extends BlockingVajram {

  public abstract CompletableFuture<Response> callAsync(EnrichedRequest request);

}

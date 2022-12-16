package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface VajramExecutor<C extends ApplicationRequestContext> extends AutoCloseable {
  <T> CompletableFuture<T> execute(
      VajramID vajramId, Function<C, VajramRequest> vajramInputProviders);
}

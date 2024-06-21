package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface VajramExecutor extends AutoCloseable {

  <T> CompletableFuture<@Nullable T> execute(
      VajramID vajramId, VajramRequest<T> vajramInputProviders);

  // Override to suppress "throws Exception"
  /** Flushes any pending requests and prevents this executor from accepting any new requests. */
  @Override
  void close();
}

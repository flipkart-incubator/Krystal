package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.vajram.VajramID;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface VajramExecutor extends AutoCloseable {

  <T> CompletableFuture<@Nullable T> execute(
      VajramID vajramId, ImmutableRequest request);

  // Override to suppress "throws Exception"
  /**
   * Flushes and executes any pending requests and prevents this executor from accepting any new
   * requests.
   */
  @Override
  void close();

  /** Abandons all pending execution requests. Any inflight requests will be terminated abruptly. */
  void shutdownNow();
}

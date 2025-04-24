package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface KrystalExecutor extends AutoCloseable {

  <T> CompletableFuture<@Nullable T> executeKryon(
      ImmutableRequest request, KryonExecutionConfig executionConfig);

  /**
   * This method starts execution of all submitted requests. No new kryon execution requests will be
   * accepted after this method is called. All requests submitted before calling this method will
   * continue execution indefinitely without interruption unless {@link #shutdownNow()} is called.
   */
  @Override // to suppress "throws Exception"
  void close();

  /** Abandons all pending execution requests. Any inflight requests will be terminated abruptly. */
  void shutdownNow();
}

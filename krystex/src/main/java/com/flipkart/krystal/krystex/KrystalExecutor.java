package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonId;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface KrystalExecutor extends AutoCloseable {

  <T> CompletableFuture<@Nullable T> executeKryon(
      KryonId kryonId, Facets facets, KryonExecutionConfig executionConfig);

  /**
   * Flushes any pending requests and waits for all those requests to finish before returning. This
   * is done so that any subsequent calls to this executor should happen only after the completion
   * of the pending futures. If this is not done, it will result in unexpected behaviour.
   */
  void flush();

  @Override // to suppress "throws Exception"
  void close();
}

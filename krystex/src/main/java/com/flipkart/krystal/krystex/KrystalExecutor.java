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
   * No new executions will be accepted after this method is called. This method starts execution of
   * all requests.
   */
  @Override // to suppress "throws Exception"
  void close();
}

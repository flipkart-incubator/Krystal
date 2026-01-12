package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.vajram.exec.VajramExecutor;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class KrystexVajramExecutor implements VajramExecutor {

  private final VajramKryonGraph vajramKryonGraph;
  private final KrystalExecutor krystalExecutor;

  @Builder
  public KrystexVajramExecutor(
      @NonNull VajramKryonGraph vajramKryonGraph,
      @NonNull KrystexVajramExecutorConfig executorConfig) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.krystalExecutor =
        new KryonExecutor(
            vajramKryonGraph.kryonDefinitionRegistry(), executorConfig.kryonExecutorConfig());
  }

  @Override
  public <T> CompletableFuture<@Nullable T> execute(ImmutableRequest<T> request) {
    return execute(request, KryonExecutionConfig.builder().build());
  }

  @Override
  public <T> void execute(RequestResponseFuture<? extends Request<T>, T> requestResponseFuture) {
    execute(requestResponseFuture, KryonExecutionConfig.builder().build());
  }

  public <T> CompletableFuture<@Nullable T> execute(
      ImmutableRequest<T> vajramRequest, KryonExecutionConfig executionConfig) {
    vajramKryonGraph.loadKryonSubGraphIfNeeded(vajramRequest._vajramID());
    RequestResponseFuture<ImmutableRequest<T>, T> requestResponseFuture =
        new RequestResponseFuture<>(vajramRequest, new CompletableFuture<>());
    execute(requestResponseFuture, executionConfig);
    return requestResponseFuture.response();
  }

  public <T> void execute(
      RequestResponseFuture<? extends Request<T>, T> requestResponseFuture,
      KryonExecutionConfig executionConfig) {
    vajramKryonGraph.loadKryonSubGraphIfNeeded(requestResponseFuture.request()._vajramID());
    krystalExecutor.executeKryon(requestResponseFuture, executionConfig);
  }

  public KrystalExecutor getKrystalExecutor() {
    return krystalExecutor;
  }

  @Override
  public void close() {
    krystalExecutor.close();
  }

  @Override
  public void shutdownNow() {
    krystalExecutor.shutdownNow();
  }
}

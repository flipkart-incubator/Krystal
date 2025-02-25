package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.core.VajramID.vajramID;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.kryondecoration.KryonExecutionContext;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.vajram.exec.VajramExecutor;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.KryonInputInjector;
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
    VajramInjectionProvider inputInjectionProvider = executorConfig.inputInjectionProvider();
    if (inputInjectionProvider != null) {
      executorConfig
          .kryonExecutorConfigBuilder()
          .requestScopedKryonDecoratorConfig(
              KryonInputInjector.DECORATOR_TYPE,
              new KryonDecoratorConfig(
                  KryonInputInjector.DECORATOR_TYPE,
                  /* shouldDecorate= */ executorContext ->
                      isInjectionNeeded(vajramKryonGraph, executorContext),
                  /* instanceIdGenerator= */ executionContext -> KryonInputInjector.DECORATOR_TYPE,
                  /* factory= */ decoratorContext ->
                      new KryonInputInjector(vajramKryonGraph, inputInjectionProvider)));
    }
    this.krystalExecutor =
        new KryonExecutor(
            vajramKryonGraph.kryonDefinitionRegistry(),
            executorConfig.kryonExecutorConfigBuilder().build(),
            executorConfig.requestId());
  }

  private static boolean isInjectionNeeded(
      VajramKryonGraph vajramKryonGraph, KryonExecutionContext executionContext) {
    return vajramKryonGraph
        .getVajramDefinition(vajramID(executionContext.vajramID().value()))
        .map(v -> v.vajramMetadata().isInputInjectionNeeded())
        .orElse(false);
  }

  @Override
  public <T> CompletableFuture<@Nullable T> execute(VajramID vajramId, ImmutableRequest request) {
    return execute(
        vajramId, request, KryonExecutionConfig.builder().executionId("defaultExecution").build());
  }

  public <T> CompletableFuture<@Nullable T> execute(
      VajramID vajramId, ImmutableRequest vajramRequest, KryonExecutionConfig executionConfig) {
    return executeWithFacets(vajramId, vajramRequest, executionConfig);
  }

  public <T> CompletableFuture<@Nullable T> executeWithFacets(
      VajramID vajramId, Request facets, KryonExecutionConfig executionConfig) {
    vajramKryonGraph.loadKryonSubGraphIfNeeded(vajramId);
    return krystalExecutor.executeKryon(vajramId, facets, executionConfig);
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

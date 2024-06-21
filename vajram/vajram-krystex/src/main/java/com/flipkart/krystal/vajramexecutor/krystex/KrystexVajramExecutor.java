package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.kryondecoration.KryonExecutionContext;
import com.flipkart.krystal.utils.MultiLeasePool;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.exec.VajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramMetadata;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.KryonInputInjector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class KrystexVajramExecutor implements VajramExecutor {

  private final VajramKryonGraph vajramKryonGraph;
  private final KrystalExecutor krystalExecutor;

  @Builder
  public KrystexVajramExecutor(
      @NonNull VajramKryonGraph vajramKryonGraph,
      MultiLeasePool<? extends ExecutorService> executorServicePool,
      @NonNull KrystexVajramExecutorConfig executorConfig) {
    this.vajramKryonGraph = vajramKryonGraph;
    InputInjectionProvider inputInjectionProvider = executorConfig.inputInjectionProvider();
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
            vajramKryonGraph.getKryonDefinitionRegistry(),
            executorServicePool,
            executorConfig.kryonExecutorConfigBuilder().build(),
            executorConfig.requestId());
  }

  private static boolean isInjectionNeeded(
      VajramKryonGraph vajramKryonGraph, KryonExecutionContext executionContext) {
    VajramMetadata vajramMetadata =
        vajramKryonGraph.getVajramMetadataMap().get(executionContext.kryonId().value());
    return vajramMetadata != null && vajramMetadata.isInputInjectionNeeded();
  }

  @Override
  public <T> CompletableFuture<@Nullable T> execute(
      VajramID vajramId, VajramRequest<T> vajramRequest) {
    return execute(
        vajramId,
        vajramRequest,
        KryonExecutionConfig.builder().executionId("defaultExecution").build());
  }

  public <T> CompletableFuture<@Nullable T> execute(
      VajramID vajramId, VajramRequest<T> vajramRequest, KryonExecutionConfig executionConfig) {
    return executeWithFacets(vajramId, vajramRequest.toFacetValues(), executionConfig);
  }

  public <T> CompletableFuture<@Nullable T> executeWithFacets(
      VajramID vajramId, Facets facets, KryonExecutionConfig executionConfig) {
    return krystalExecutor.executeKryon(
        vajramKryonGraph.getKryonId(vajramId), facets, executionConfig);
  }

  public KrystalExecutor getKrystalExecutor() {
    return krystalExecutor;
  }

  @Override
  public void close() {
    krystalExecutor.close();
  }
}

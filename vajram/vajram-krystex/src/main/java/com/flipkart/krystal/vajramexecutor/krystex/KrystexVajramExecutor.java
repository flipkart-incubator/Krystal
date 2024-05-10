package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.utils.MultiLeasePool;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.exec.VajramExecutor;
import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class KrystexVajramExecutor<C extends ApplicationRequestContext>
    implements VajramExecutor<C> {

  private final VajramKryonGraph vajramKryonGraph;
  private final C applicationRequestContext;
  private final KrystalExecutor krystalExecutor;

  public KrystexVajramExecutor(
      VajramKryonGraph vajramKryonGraph,
      C applicationRequestContext,
      MultiLeasePool<? extends ExecutorService> executorServicePool,
      KryonExecutorConfig config) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.applicationRequestContext = applicationRequestContext;
    this.krystalExecutor =
        new KryonExecutor(
            vajramKryonGraph.getKryonDefinitionRegistry(),
            executorServicePool,
            config,
            applicationRequestContext.requestId());
  }

  @Override
  public <T> CompletableFuture<@Nullable T> execute(
      VajramID vajramId, Function<C, VajramRequest<T>> vajramRequestCreator) {
    return execute(
        vajramId,
        vajramRequestCreator,
        KryonExecutionConfig.builder().executionId("defaultExecution").build());
  }

  public <T> CompletableFuture<@Nullable T> execute(
      VajramID vajramId,
      Function<C, VajramRequest<T>> vajramRequestCreator,
      KryonExecutionConfig executionConfig) {
    return executeWithFacets(
        vajramId, vajramRequestCreator.andThen(FacetValuesAdaptor::toFacetValues), executionConfig);
  }

  public <T> CompletableFuture<@Nullable T> executeWithFacets(
      VajramID vajramId, Function<C, Facets> facetsCreator, KryonExecutionConfig executionConfig) {
    return krystalExecutor.executeKryon(
        vajramKryonGraph.getKryonId(vajramId),
        facetsCreator.apply(applicationRequestContext),
        executionConfig);
  }

  public KrystalExecutor getKrystalExecutor() {
    return krystalExecutor;
  }

  @Override
  public void flush() {
    krystalExecutor.flush();
  }

  @Override
  public void close() {
    krystalExecutor.close();
  }
}

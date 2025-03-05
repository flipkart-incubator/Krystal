package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.core.VajramID.vajramID;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecoratorConfig;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.kryondecoration.KryonExecutionContext;
import com.flipkart.krystal.traits.TraitBindingProvider;
import com.flipkart.krystal.vajram.exec.VajramExecutor;
import com.flipkart.krystal.vajram.facets.TraitDependency;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.KryonInputInjector;
import com.flipkart.krystal.vajramexecutor.krystex.traitbinding.TraitBindingDecorator;
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
          .kryonDecoratorConfig(
              KryonInputInjector.DECORATOR_TYPE,
              new KryonDecoratorConfig(
                  KryonInputInjector.DECORATOR_TYPE,
                  /* shouldDecorate= */ executorContext ->
                      isInjectionNeeded(vajramKryonGraph, executorContext),
                  /* instanceIdGenerator= */ executionContext -> KryonInputInjector.DECORATOR_TYPE,
                  /* factory= */ decoratorContext ->
                      new KryonInputInjector(vajramKryonGraph, inputInjectionProvider)));
    }
    TraitBindingProvider traitBindingProvider = executorConfig.traitBindingProvider();
    if (traitBindingProvider != null) {
      executorConfig
          .kryonExecutorConfigBuilder()
          .dependencyDecoratorConfig(
              TraitBindingDecorator.DECORATOR_TYPE,
              new DependencyDecoratorConfig(
                  TraitBindingDecorator.DECORATOR_TYPE,
                  dependencyExecutionContext ->
                      isTraitBindingNeeded(vajramKryonGraph, dependencyExecutionContext),
                  d -> TraitBindingDecorator.DECORATOR_TYPE,
                  c -> new TraitBindingDecorator(traitBindingProvider)));
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
        .getVajramDefinition(executionContext.vajramID())
        .vajramMetadata()
        .isInputInjectionNeeded();
  }

  private static boolean isTraitBindingNeeded(
      VajramKryonGraph vajramKryonGraph, DependencyExecutionContext executionContext) {
    FacetSpec facetSpec = vajramKryonGraph
        .getVajramDefinition(executionContext.vajramID())
        .facetsByName()
        .get(executionContext.dependency().name());
    if(facetSpec == null){
      return false;
    }
    return facetSpec
        .tags()
        .getAnnotationByType(TraitDependency.class)
        .isPresent();
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

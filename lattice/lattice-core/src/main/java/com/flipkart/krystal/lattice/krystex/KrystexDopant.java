package com.flipkart.krystal.lattice.krystex;

import static com.flipkart.krystal.krystex.batching.DepChainBatcherConfig.computeSharedBatcherConfig;
import static com.flipkart.krystal.lattice.krystex.KrystexDopant.DOPANT_TYPE;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.KrystalExecutorConfig.KrystalExecutorConfigBuilder;
import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.batching.DepChainBatcherConfig.BatchSizeSupplier;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionFramework;
import com.flipkart.krystal.lattice.core.di.Produces;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant;
import com.flipkart.krystal.lattice.vajram.RequestInitializer;
import com.flipkart.krystal.lattice.vajram.VajramRequestExecutionContext;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.builder.qual.CalledMethods;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
@DopantType(DOPANT_TYPE)
@Singleton
public final class KrystexDopant implements SimpleDopant {
  public static final String DOPANT_TYPE = "krystal.lattice.krystex";

  @Getter(onMethod_ = @Produces(inScope = Singleton.class))
  private final KrystexGraph krystexGraph;

  private final ThreadingStrategyDopant threadingStrategyDopant;
  private final KryonExecutorConfigurator kryonExecutorConfigurator;

  @Inject
  public KrystexDopant(
      KrystexDopantSpec krystexDopantSpec,
      DependencyInjectionFramework dependencyInjectionFramework,
      VajramGraph vajramGraph,
      ThreadingStrategyDopant threadingStrategyDopant) {
    this.threadingStrategyDopant = threadingStrategyDopant;
    KrystexGraphBuilder krystexGraphBuilder = KrystexGraph.builder();
    for (Consumer<KrystexGraphBuilder> p : krystexDopantSpec.buildKrystexGraphWith()) {
      p.accept(krystexGraphBuilder);
    }
    this.kryonExecutorConfigurator =
        configBuilder ->
            krystexDopantSpec.configureExecutorWith().forEach(configBuilder::configureWith);

    TraitDispatchPolicies traitDispatchPolicies =
        new TraitDispatchPolicies(krystexDopantSpec.traitDispatchPolicies());
    KrystexGraphBuilder graphBuilder =
        krystexGraphBuilder
            .vajramGraph(vajramGraph)
            .injectionProvider(dependencyInjectionFramework.toVajramInjectionProvider())
            .traitDispatchPolicies(traitDispatchPolicies);
    if (krystexDopantSpec.enableSharedAutoBatchers()) {
      BatchSizeSupplier batchSizeSupplier = krystexDopantSpec.batchSizeSupplier();
      if (batchSizeSupplier != null) {
        krystexGraphBuilder.inputBatcherConfig(
            computeSharedBatcherConfig(vajramGraph, batchSizeSupplier, traitDispatchPolicies));
      }
    }
    this.krystexGraph = graphBuilder.build();
  }

  public <RespT extends @Nullable Object> CompletionStage<RespT> executeRequest(
      VajramRequestExecutionContext<RespT> executionContext) throws LeaseUnavailableException {
    ImmutableRequest<RespT> vajramRequest = executionContext.vajramRequest();
    Bindings requestScopeSeeds = executionContext.requestScopeSeeds();
    KrystalExecutorConfigBuilder executorConfigBuilder = executionContext.executorConfigBuilder();
    Lease<? extends ExecutorService> lease = threadingStrategyDopant.getExecutorService();
    ExecutorService executorService = lease.get();
    if (!(executorService instanceof SingleThreadExecutor singleThreadExecutor)) {
      throw new UnsupportedOperationException(
          "Expected 'SingleThreadExecutor'. Found " + executorService.getClass());
    }

    ExecutorService transformedExecutor =
        threadingStrategyDopant.executorServiceTransformer().apply(singleThreadExecutor);
    @SuppressWarnings("assignment")
    CompletableFuture<RespT> future =
        CompletableFuture.supplyAsync(
                () -> {
                  List<AutoCloseable> requestScopedCloseables = new ArrayList<>();
                  var requestScope = threadingStrategyDopant.openRequestScope(requestScopeSeeds);
                  requestScopedCloseables.add(requestScope);
                  requestScopedCloseables.add(lease);
                  for (RequestInitializer requestInitializer :
                      executionContext.requestScopeInitializers()) {
                    requestScopedCloseables.add(requestInitializer.init());
                  }
                  try (VajramKryonExecutor executor =
                      createExecutor(
                          executorConfigBuilder
                              .executorService(singleThreadExecutor)
                              .executorServiceTransformer(
                                  threadingStrategyDopant.executorServiceTransformer()))) {
                    return executor
                        .execute(vajramRequest)
                        .whenComplete((response, throwable) -> closeAll(requestScopedCloseables))
                        .whenComplete(
                            (respT, throwable) -> {
                              if (throwable == null) {
                                if (log.isInfoEnabled()) {
                                  log.info(
                                      "Request sent to executor id '{}' completed successfully",
                                      executor.executorId());
                                }
                              } else {
                                if (log.isErrorEnabled()) {
                                  log.error(
                                      "Request sent to executor id '{}' completed with error",
                                      executor.executorId(),
                                      throwable);
                                }
                              }
                            });
                  } catch (Exception e) {
                    closeAll(requestScopedCloseables);
                    return CompletableFuture.<@Nullable RespT>failedFuture(e);
                  }
                },
                transformedExecutor)
            .thenCompose(f -> f);
    return future;
  }

  private VajramKryonExecutor createExecutor(
      @CalledMethods("executorService") KrystalExecutorConfigBuilder kryonConfigBuilder) {
    kryonConfigBuilder.configureWith(kryonExecutorConfigurator);
    return krystexGraph.createExecutor(kryonConfigBuilder);
  }

  private static void closeAll(List<AutoCloseable> closeables) {
    for (AutoCloseable closeable : closeables) {
      try {
        closeable.close();
      } catch (Throwable e) {
        log.error("Unable to execute initializer closeable", e);
      }
    }
  }
}

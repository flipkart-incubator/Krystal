package com.flipkart.krystal.lattice.krystex;

import static com.flipkart.krystal.lattice.core.di.Util.asOptional;
import static com.flipkart.krystal.lattice.krystex.KrystexDopant.DOPANT_TYPE;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionFramework;
import com.flipkart.krystal.lattice.core.di.Produces;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant;
import com.flipkart.krystal.lattice.vajram.RequestInitializer;
import com.flipkart.krystal.lattice.vajram.VajramDopant;
import com.flipkart.krystal.lattice.vajram.VajramRequestExecutionContext;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.traits.TraitDispatchPolicies.TraitDispatchPoliciesBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
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
      VajramDopant vajramDopant,
      ThreadingStrategyDopant threadingStrategyDopant,
      Provider<KrystexGraphBuilder> krystexGraphBuilderProvider,
      Provider<KryonExecutorConfigurator> kryonExecutorConfigurator,
      Provider<TraitDispatchPolicies> traitDispatchPolicies) {
    this.threadingStrategyDopant = threadingStrategyDopant;
    KrystexGraphBuilder krystexGraphBuilder =
        asOptional(krystexGraphBuilderProvider)
            .orElse(KrystexGraph.builder())
            .vajramGraph(vajramDopant.vajramGraph())
            .injectionProvider(dependencyInjectionFramework.toVajramInjectionProvider());
    asOptional(traitDispatchPolicies).ifPresent(krystexGraphBuilder::traitDispatchPolicies);
    for (Consumer<KrystexGraphBuilder> p : krystexDopantSpec.buildKrystexGraphWith()) {
      p.accept(krystexGraphBuilder);
    }
    this.kryonExecutorConfigurator =
        configBuilder -> {
          configBuilder.configureWith(
              asOptional(kryonExecutorConfigurator).orElse(KryonExecutorConfigurator.NO_OP));
          krystexDopantSpec.configureExecutorWith().forEach(configBuilder::configureWith);
        };
    this.krystexGraph = krystexGraphBuilder.build();
  }

  @Produces(inScope = Singleton.class)
  public static TraitDispatchPolicies traitDispatchPolicies(
      Provider<TraitDispatchPoliciesBuilder> traitDispatchPoliciesBuilder,
      KrystexDopantSpec krystexDopantSpec) {
    return asOptional(traitDispatchPoliciesBuilder)
        .orElseGet(TraitDispatchPolicies::builder)
        .addTraitDispatchPolicies(krystexDopantSpec.traitDispatchPolicies())
        .build();
  }

  public <RespT extends @Nullable Object> CompletionStage<RespT> executeRequest(
      VajramRequestExecutionContext<RespT> executionContext) throws LeaseUnavailableException {
    ImmutableRequest<RespT> vajramRequest = executionContext.vajramRequest();
    Bindings requestScopeSeeds = executionContext.requestScopeSeeds();
    KryonExecutorConfigBuilder executorConfigBuilder = executionContext.executorConfigBuilder();
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
                  try (KrystexVajramExecutor executor =
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
                                      executor.getKrystalExecutor().executorId());
                                }
                              } else {
                                if (log.isErrorEnabled()) {
                                  log.error(
                                      "Request sent to executor id '{}' completed with error",
                                      executor.getKrystalExecutor().executorId(),
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

  private KrystexVajramExecutor createExecutor(
      @CalledMethods("executorService") KryonExecutorConfigBuilder kryonConfigBuilder) {
    kryonConfigBuilder.configureWith(kryonExecutorConfigurator);

    return krystexGraph.createExecutor(
        KrystexVajramExecutorConfig.builder().kryonExecutorConfig(kryonConfigBuilder.build()));
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

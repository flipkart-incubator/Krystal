package com.flipkart.krystal.lattice.vajram;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant;
import com.flipkart.krystal.lattice.vajram.VajramDopantSpec.VajramDopantSpecBuilder;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.builder.qual.CalledMethods;
import org.checkerframework.checker.nullness.qual.Nullable;

@Singleton
@Slf4j
@DopantType(VajramDopant.DOPANT_TYPE)
public final class VajramDopant implements SimpleDopant {

  static final String DOPANT_TYPE = "krystal.lattice.vajram";

  private final VajramDopantSpec vajramDopantSpec;
  private final VajramKryonGraph vajramGraph;
  private final ThreadingStrategyDopant threadingStrategyDopant;

  @Inject
  VajramDopant(
      VajramDopantSpec vajramDopantSpec,
      DependencyInjectionBinder injectionBinder,
      ThreadingStrategyDopant threadingStrategyDopant) {
    this.vajramDopantSpec = vajramDopantSpec;
    this.vajramGraph = vajramDopantSpec.vajramGraph();
    this.threadingStrategyDopant = threadingStrategyDopant;
    VajramInjectionProvider vajramInjectionProvider = injectionBinder.toVajramInjectionProvider();
    if (vajramInjectionProvider != null) {
      this.vajramGraph.registerInputInjector(vajramInjectionProvider);
    }
  }

  public static VajramDopantSpecBuilder vajramGraph() {
    return new VajramDopantSpecBuilder();
  }

  public KrystexVajramExecutor createExecutor(
      @CalledMethods("executorService") KryonExecutorConfigBuilder kryonConfigBuilder) {
    vajramDopantSpec.kryonExecutorConfigurators().forEach(m -> m.addToConfig(kryonConfigBuilder));

    return vajramGraph.createExecutor(
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(kryonConfigBuilder)
            .build());
  }

  public <RespT extends @Nullable Object> CompletionStage<RespT> executeRequest(
      VajramRequestExecutionContext<RespT> executionContext) throws LeaseUnavailableException {
    ImmutableRequest<RespT> vajramRequest = executionContext.vajramRequest();
    Bindings requestScopeSeeds = executionContext.requestScopeSeeds();
    KryonExecutorConfigBuilder executorConfigBuilder = executionContext.executorConfigBuilder();
    Lease<? extends ExecutorService> lease;
    lease = threadingStrategyDopant.getExecutorService();
    ExecutorService executorService = lease.get();
    if (!(executorService instanceof SingleThreadExecutor singleThreadExecutor)) {
      throw new UnsupportedOperationException(
          "Expected 'SingleThreadExecutor'. Found " + executorService.getClass());
    }
    @SuppressWarnings("assignment")
    CompletableFuture<RespT> future =
        CompletableFuture.supplyAsync(
                () -> {
                  List<AutoCloseable> initCloseables = new ArrayList<>();
                  for (RequestInitializer requestInitializer :
                      executionContext.requestScopeInitializers()) {
                    initCloseables.add(requestInitializer.init());
                  }
                  Closeable requestScope =
                      threadingStrategyDopant.openRequestScope(requestScopeSeeds);
                  try (KrystexVajramExecutor executor =
                      createExecutor(executorConfigBuilder.executorService(singleThreadExecutor))) {
                    return executor
                        .execute(vajramRequest)
                        .whenComplete(
                            (response, throwable) -> {
                              try {
                                requestScope.close();
                              } catch (Throwable e) {
                                log.error("Unable to close request scope", e);
                              }
                              try {
                                lease.close();
                              } catch (Throwable e) {
                                log.error("Unable to close executor Service lease", e);
                              }
                              for (AutoCloseable closeable : initCloseables) {
                                try {
                                  closeable.close();
                                } catch (Throwable e) {
                                  log.error("Unable to execute initializer closeable", e);
                                }
                              }
                            });
                  }
                },
                singleThreadExecutor)
            .thenCompose(f -> f);
    return future;
  }
}

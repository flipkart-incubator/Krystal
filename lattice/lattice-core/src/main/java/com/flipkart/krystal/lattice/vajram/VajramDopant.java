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
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

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

  public KrystexVajramExecutor createExecutor(KryonExecutorConfigBuilder kryonConfigBuilder) {
    vajramDopantSpec.kryonExecutorConfigurators().forEach(m -> m.addToConfig(kryonConfigBuilder));

    return vajramGraph.createExecutor(
        KrystexVajramExecutorConfig.builder()
            .kryonExecutorConfigBuilder(kryonConfigBuilder)
            .build());
  }

  public <RespT> CompletionStage<RespT> executeRequest(
      ImmutableRequest<RespT> vajramRequest,
      Bindings requestScopeSeeds,
      KryonExecutorConfigBuilder executorConfigBuilder)
      throws LeaseUnavailableException {
    Lease<? extends ExecutorService> lease;
    lease = threadingStrategyDopant.getExecutorService();
    ExecutorService executorService = lease.get();
    if (!(executorService instanceof SingleThreadExecutor singleThreadExecutor)) {
      throw new UnsupportedOperationException(
          "Expected 'SingleThreadExecutor'. Found " + executorService.getClass());
    }
    return CompletableFuture.supplyAsync(
            () -> {
              Closeable requestScope = threadingStrategyDopant.openRequestScope(requestScopeSeeds);
              try (KrystexVajramExecutor executor =
                  createExecutor(executorConfigBuilder.executor(singleThreadExecutor))) {
                return executor
                    .execute(vajramRequest)
                    .whenComplete(
                        (response, throwable) -> {
                          try {
                            requestScope.close();
                          } catch (IOException e) {
                            log.error("Unable to close request scope");
                          }
                          try {
                            lease.close();
                          } catch (Exception e) {
                            log.error("Unable to close executor Service lease");
                          }
                        });
              }
            },
            singleThreadExecutor)
        .thenCompose(Function.identity());
  }
}

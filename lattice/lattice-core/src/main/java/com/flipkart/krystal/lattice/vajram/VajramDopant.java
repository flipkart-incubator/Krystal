package com.flipkart.krystal.lattice.vajram;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.core.doping.SimpleDopant;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig.KrystexVajramExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import jakarta.inject.Inject;
import java.util.List;
import java.util.function.Consumer;

@DopantType(VajramDopant.DOPANT_TYPE)
public final class VajramDopant implements SimpleDopant {

  static final String DOPANT_TYPE = "krystal.lattice.vajram";

  private final VajramDopantSpec vajramDopantSpec;
  private final VajramKryonGraph vajramGraph;

  @Inject
  VajramDopant(VajramDopantSpec vajramDopantSpec, DependencyInjectionBinder injectionBinder) {
    this.vajramDopantSpec = vajramDopantSpec;
    this.vajramGraph = vajramDopantSpec.vajramGraph();
    VajramInjectionProvider vajramInjectionProvider = injectionBinder.toVajramInjectionProvider();
    if (vajramInjectionProvider != null) {
      this.vajramGraph.registerInputInjector(vajramInjectionProvider);
    }
  }

  public static VajramDopantSpecBuilder vajramGraph() {
    return new VajramDopantSpecBuilder();
  }

  public KrystexVajramExecutor createExecutor(SingleThreadExecutor executorService) {
    return createExecutor(executorService, List.of(), List.of());
  }

  public KrystexVajramExecutor createExecutor(
      SingleThreadExecutor executorService,
      List<KryonExecutorConfigurator> kryonExecutorConfigurators,
      List<Consumer<KrystexVajramExecutorConfigBuilder>> krystexExecutorConfigurators) {
    var kryonConfigBuilder = KryonExecutorConfig.builder().executor(executorService);

    kryonExecutorConfigurators.forEach(m -> m.addToConfig(kryonConfigBuilder));
    vajramDopantSpec.kryonExecutorConfigurators().forEach(m -> m.addToConfig(kryonConfigBuilder));

    var vajramConfigBuilder = KrystexVajramExecutorConfig.builder();
    krystexExecutorConfigurators.forEach(p -> p.accept(vajramConfigBuilder));
    vajramDopantSpec.krystexExecutorConfigurators().forEach(p -> p.accept(vajramConfigBuilder));

    return vajramGraph.createExecutor(
        vajramConfigBuilder.kryonExecutorConfigBuilder(kryonConfigBuilder).build());
  }
}

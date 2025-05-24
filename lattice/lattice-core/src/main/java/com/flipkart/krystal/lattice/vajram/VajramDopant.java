package com.flipkart.krystal.lattice.vajram;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import jakarta.inject.Inject;

@DopantType(VajramDopant.DOPANT_TYPE)
public final class VajramDopant implements Dopant<NoAnnotation, NoConfiguration> {

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

  public static VajramDopantSpecBuilder vajramDopant() {
    return new VajramDopantSpecBuilder();
  }

  public KrystexVajramExecutor createExecutor(SingleThreadExecutor executorService) {
    var kryonConfigBuilder = KryonExecutorConfig.builder().executor(executorService);
    vajramDopantSpec.kryonExecutorConfigurators().forEach(m -> m.addToConfig(kryonConfigBuilder));

    var vajramConfigBuilder = KrystexVajramExecutorConfig.builder();
    vajramDopantSpec.kryonExecConfigProcessors().forEach(p -> p.accept(vajramConfigBuilder));

    return vajramGraph.createExecutor(
        vajramConfigBuilder.kryonExecutorConfigBuilder(kryonConfigBuilder).build());
  }
}

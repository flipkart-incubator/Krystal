package com.flipkart.krystal.lattice.vajram;

import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import jakarta.inject.Inject;

public final class VajramGraphDopant
    implements Dopant<NoAnnotation, NoConfiguration> {

  private final VajramGraphSpec vajramGraphSpec;
  private final VajramKryonGraph vajramGraph;

  @Inject
  VajramGraphDopant(VajramGraphSpec vajramGraphSpec, DependencyInjectionBinder injectionBinder) {
    this.vajramGraphSpec = vajramGraphSpec;
    this.vajramGraph = vajramGraphSpec.vajramGraph();
    VajramInjectionProvider vajramInjectionProvider = injectionBinder.toVajramInjectionProvider();
    if (vajramInjectionProvider != null) {
      this.vajramGraph.registerInputInjector(vajramInjectionProvider);
    }
  }

  public static VajramGraphSpecBuilder vajramDopant() {
    return new VajramGraphSpecBuilder();
  }

  public KrystexVajramExecutor createExecutor(SingleThreadExecutor executorService) {
    var kryonConfigBuilder = KryonExecutorConfig.builder().executor(executorService);
    vajramGraphSpec.kryonExecutorConfigurators().forEach(m -> m.addToConfig(kryonConfigBuilder));

    var vajramConfigBuilder = KrystexVajramExecutorConfig.builder();
    vajramGraphSpec.kryonExecConfigProcessors().forEach(p -> p.accept(vajramConfigBuilder));

    return vajramGraph.createExecutor(
        vajramConfigBuilder.kryonExecutorConfigBuilder(kryonConfigBuilder).build());
  }
}

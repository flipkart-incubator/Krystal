package com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.app;

import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.VajramGraph.VajramGraphBuilder;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.Proto2024eLatticeSample;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jakarta.inject.Singleton;

class CustomGuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(CustomInjectionSample.class).toInstance(new CustomInjectionSample() {});
    bind(KryonExecutorConfigurator.class).toInstance(KryonExecutorConfigurator.NO_OP);
  }

  @Provides
  @Singleton
  public VajramGraphBuilder vajramGraphBuilder() {
    return VajramGraph.builder()
        .loadFromPackage("com.flipkart.krystal.lattice.samples.proto2024e.sampleProtoService.app")
        .loadClasses(Proto2024eLatticeSample.class);
  }

  @Provides
  @Singleton
  public KrystexGraphBuilder krystexGraphBuilder() {
    return KrystexGraph.builder();
  }
}

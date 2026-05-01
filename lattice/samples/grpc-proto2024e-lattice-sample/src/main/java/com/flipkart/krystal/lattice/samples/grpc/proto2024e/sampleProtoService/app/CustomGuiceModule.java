package com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService.app;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.google.inject.AbstractModule;

class CustomGuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(CustomInjectionSample.class).toInstance(new CustomInjectionSample() {});
    bind(KryonExecutorConfigurator.class).toInstance(KryonExecutorConfigurator.NO_OP);
  }
}

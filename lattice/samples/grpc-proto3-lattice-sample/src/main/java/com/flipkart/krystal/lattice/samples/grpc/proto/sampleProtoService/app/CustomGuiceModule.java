package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService.app;

import com.google.inject.AbstractModule;

class CustomGuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(CustomInjectionSample.class).toInstance(new CustomInjectionSample() {});
  }
}

package com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.app;

import com.google.inject.AbstractModule;

class CustomGuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(CustomInjectionSample.class).toInstance(new CustomInjectionSample() {});
  }
}

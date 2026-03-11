package com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.app;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.google.inject.AbstractModule;

public class RestfulDropWizardAppModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(KryonExecutorConfigurator.class).toInstance(KryonExecutorConfigurator.NO_OP);
  }
}

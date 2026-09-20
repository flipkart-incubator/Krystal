package com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.app;

import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.VajramGraph.VajramGraphBuilder;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestGetMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestHeadMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestPostComplexPathMatching;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestPostMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.logic.RestStreamingSample;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jakarta.inject.Singleton;

public class RestfulDropWizardAppModule extends AbstractModule {

  @Provides
  @Singleton
  public VajramGraphBuilder vajramGraphBuilder() {
    return VajramGraph.builder()
        .loadClasses(
            RestLatticeSample.class,
            RestGetMappingLatticeSample.class,
            RestPostMappingLatticeSample.class,
            RestPostComplexPathMatching.class,
            RestHeadMappingLatticeSample.class,
            RestStreamingSample.class);
  }

  @Provides
  @Singleton
  public KrystexGraphBuilder krystexGraphBuilder() {
    return KrystexGraph.builder();
  }
}

package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.app;

import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.VajramGraph.VajramGraphBuilder;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.logic.ForyGetSample;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.logic.ForyPostSample;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ForyRestAppProducer {
  @Produces
  @ApplicationScoped
  public VajramGraphBuilder vajramGraphBuilder() {
    return VajramGraph.builder().loadClasses(ForyGetSample.class, ForyPostSample.class);
  }

  @Produces
  @ApplicationScoped
  public KrystexGraphBuilder krystexGraphBuilder() {
    return KrystexGraph.builder();
  }
}

package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.app;

import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.VajramGraph.VajramGraphBuilder;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic.RestGetMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic.RestHeadMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic.RestLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic.RestPostComplexPathMatching;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic.RestPostMappingLatticeSample;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.logic.RestStreamingSample;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class RestfulQuarkusAppProducer {
  @Produces
  @ApplicationScoped
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

  @Produces
  @ApplicationScoped
  public KrystexGraphBuilder krystexGraphBuilder() {
    return KrystexGraph.builder();
  }
}

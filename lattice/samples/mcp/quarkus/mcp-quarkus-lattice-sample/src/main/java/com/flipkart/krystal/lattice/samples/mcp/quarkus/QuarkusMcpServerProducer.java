package com.flipkart.krystal.lattice.samples.mcp.quarkus;

import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.VajramGraph.VajramGraphBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class QuarkusMcpServerProducer {
  @Produces
  @ApplicationScoped
  public VajramGraphBuilder vajramGraphBuilder() {
    return VajramGraph.builder()
        .loadFromPackage("com.flipkart.krystal.lattice.samples.mcp.quarkus.logic");
  }

  @Produces
  @ApplicationScoped
  public KrystexGraphBuilder krystexGraphBuilder() {
    return KrystexGraph.builder();
  }
}

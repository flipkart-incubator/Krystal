package com.flipkart.krystal.lattice.samples.graphql.rest.json;

import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.VajramGraph.VajramGraphBuilder;
import com.flipkart.krystal.lattice.graphql.rest.restapi.HttpPostGraphQl;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class SampleGraphQlServerAppProducer {
  @Produces
  @ApplicationScoped
  public VajramGraphBuilder vajramGraphBuilder() {
    return VajramGraph.builder()
        .loadFromPackage("com.flipkart.krystal.lattice.samples.graphql.rest.json")
        .loadClasses(GraphQlOperationAggregate.class, HttpPostGraphQl.class);
  }

  @Produces
  @ApplicationScoped
  public KrystexGraphBuilder krystexGraphBuilder() {
    return KrystexGraph.builder();
  }
}

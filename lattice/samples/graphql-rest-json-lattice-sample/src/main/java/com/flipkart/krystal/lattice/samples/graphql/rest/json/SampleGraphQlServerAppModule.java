package com.flipkart.krystal.lattice.samples.graphql.rest.json;

import static com.flipkart.krystal.vajramexecutor.krystex.batching.DepChainBatcherConfig.computeSharedBatcherConfig;

import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class SampleGraphQlServerAppModule {
  @Produces
  @ApplicationScoped
  public KrystexGraphBuilder krystexGraph(
      VajramGraph vajramGraph, TraitDispatchPolicies traitDispatchPolicies) {
    return KrystexGraph.builder()
        .inputBatcherConfig(
            computeSharedBatcherConfig(vajramGraph, vajramId -> 10, traitDispatchPolicies));
  }
}

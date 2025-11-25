package com.flipkart.krystal.lattice.samples.graphql.rest.json;

import com.flipkart.krystal.lattice.graphql.rest.dopant.GraphQlOverRestDopant;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.samplequery.SampleQuery_GQlAggr_Req;
import jakarta.inject.Inject;
import java.util.Set;

public final class SampleGraphQlServer_GraphQlOverRestDopant_Impl extends GraphQlOverRestDopant {

  @Inject
  public SampleGraphQlServer_GraphQlOverRestDopant_Impl(GraphQlOverRestDopantInitData initData) {
    super(initData, Set.of(SampleQuery_GQlAggr_Req.class));
  }
}

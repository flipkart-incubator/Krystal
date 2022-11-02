package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.das.GraphQl;
import com.google.common.collect.ImmutableMap;

final class GraphQlIndex implements AccessSpecIndex<GraphQl> {

  @Override
  public ImmutableMap<GraphQl, Vajram<?>> getVajrams(GraphQl accessSpec) {
    //TODO implement GraphQL matching logic and Data structures
    return null;
  }

  @Override
  public void add(Vajram<?> vajram) {
    //TODO Implement vajram addition
  }
}

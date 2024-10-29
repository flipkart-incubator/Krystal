package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.resolution.ResolverCommand;

public non-sealed interface FanoutInputResolver extends InputResolver {
  ResolverCommand resolve(ImmutableRequest<Object> depRequest, Facets facets);
//  ResolverCommand resolve( Facets facets); //TODO

  @Override
  default boolean canFanout() {
    return true;
  }
}

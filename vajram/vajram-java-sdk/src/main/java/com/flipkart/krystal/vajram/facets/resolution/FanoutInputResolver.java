package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.facets.resolution.ResolverCommand;

public non-sealed interface FanoutInputResolver extends InputResolver {
  ResolverCommand resolve(Builder depRequest, Facets facets);
}

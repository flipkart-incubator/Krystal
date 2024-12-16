package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.resolution.ResolverCommand;
import com.google.common.collect.ImmutableList;

public sealed interface SingleInputResolver extends InputResolver
    permits AbstractSingleInputResolver, SimpleSingleInputResolver {
  ResolverCommand resolve(ImmutableList<? extends RequestBuilder<?>> depRequests, Facets facets);

  //  ResolverCommand resolve(RequestBuilder<Object> depRequests, Facets facets);

  @Override
  default boolean canFanout() {
    return false;
  }
}

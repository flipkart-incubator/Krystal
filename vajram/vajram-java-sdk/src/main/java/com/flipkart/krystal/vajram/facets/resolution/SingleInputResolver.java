package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.google.common.collect.ImmutableList;

public sealed interface SingleInputResolver extends InputResolver
    permits AbstractOne2OneInputResolver, SimpleSingleInputResolver {
  ResolverCommand resolve(ImmutableList<? extends RequestBuilder> depRequests, Facets facets);
}

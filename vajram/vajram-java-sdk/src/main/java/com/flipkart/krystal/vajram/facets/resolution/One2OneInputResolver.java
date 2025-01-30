package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.google.common.collect.ImmutableList;

public sealed interface One2OneInputResolver extends InputResolver
    permits AbstractOne2OneInputResolver, SimpleOne2OneInputResolver {
  ResolverCommand resolve(ImmutableList<? extends Builder> depRequests, Facets facets);
}

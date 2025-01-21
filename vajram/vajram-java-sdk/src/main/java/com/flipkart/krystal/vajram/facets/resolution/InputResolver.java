package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.facets.resolution.ResolverDefinition;

public sealed interface InputResolver
    permits AbstractInputResolver, FanoutInputResolver, SimpleInputResolver, SingleInputResolver {
  ResolverDefinition definition();
}

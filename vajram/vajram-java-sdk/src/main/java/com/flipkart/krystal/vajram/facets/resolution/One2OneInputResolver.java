package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.google.common.collect.ImmutableList;
import java.util.List;

public sealed interface One2OneInputResolver extends InputResolver
    permits AbstractOne2OneInputResolver, SimpleOne2OneInputResolver {
  ResolverCommand resolve(
      List<? extends Builder<?>> _depRequests,
      FacetValues _rawFacetValues);
}

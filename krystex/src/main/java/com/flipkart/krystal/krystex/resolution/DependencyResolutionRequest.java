package com.flipkart.krystal.krystex.resolution;

import static java.util.Collections.unmodifiableSet;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import java.util.Set;

public record DependencyResolutionRequest(
    Facet dependencyId, Set<ResolverDefinition> resolverDefs) {
  public DependencyResolutionRequest {
    resolverDefs = unmodifiableSet(resolverDefs);
  }
}

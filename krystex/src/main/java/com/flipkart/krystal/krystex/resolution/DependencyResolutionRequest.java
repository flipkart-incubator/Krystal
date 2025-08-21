package com.flipkart.krystal.krystex.resolution;

import static java.util.Collections.unmodifiableSet;

import java.util.Set;

public record DependencyResolutionRequest(
    String dependencyName, Set<ResolverDefinition> resolverDefinitions) {

  public DependencyResolutionRequest {
    resolverDefinitions = unmodifiableSet(resolverDefinitions);
  }
}

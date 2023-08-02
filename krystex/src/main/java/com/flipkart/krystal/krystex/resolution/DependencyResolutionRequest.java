package com.flipkart.krystal.krystex.resolution;

import java.util.Set;

public record DependencyResolutionRequest(
    String dependencyName, Set<ResolverDefinition> resolverDefinitions) {}

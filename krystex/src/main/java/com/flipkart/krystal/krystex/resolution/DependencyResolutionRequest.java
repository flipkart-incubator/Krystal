package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.google.common.collect.ImmutableSet;

public record DependencyResolutionRequest(
    Facet dependencyId, ImmutableSet<ResolverDefinition> resolverDefs) {}

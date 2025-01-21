package com.flipkart.krystal.facets.resolution;

import com.flipkart.krystal.facets.Facet;
import com.google.common.collect.ImmutableSet;

public record ResolverDefinition(
    ImmutableSet<? extends Facet> sources, ResolutionTarget target, boolean canFanout) {}

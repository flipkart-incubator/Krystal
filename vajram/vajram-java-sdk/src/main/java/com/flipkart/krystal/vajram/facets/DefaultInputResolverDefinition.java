package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.vajram.facets.resolution.InputResolverDefinition;
import com.google.common.collect.ImmutableSet;

public record DefaultInputResolverDefinition(
    int resolverId, ImmutableSet<Integer> sources, QualifiedInputs resolutionTarget)
    implements InputResolverDefinition {}

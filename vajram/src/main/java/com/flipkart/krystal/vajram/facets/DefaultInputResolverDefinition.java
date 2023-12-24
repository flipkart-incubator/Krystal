package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.vajram.facets.resolution.InputResolverDefinition;
import com.google.common.collect.ImmutableSet;

public record DefaultInputResolverDefinition(
    ImmutableSet<String> sources, QualifiedInputs resolutionTarget)
    implements InputResolverDefinition {}

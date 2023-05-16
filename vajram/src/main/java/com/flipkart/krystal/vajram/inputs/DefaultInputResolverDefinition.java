package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.inputs.resolution.InputResolverDefinition;
import com.google.common.collect.ImmutableSet;

public record DefaultInputResolverDefinition(
    ImmutableSet<String> sources, QualifiedInputs resolutionTarget)
    implements InputResolverDefinition {}

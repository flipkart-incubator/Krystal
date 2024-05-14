package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.vajram.facets.DefaultInputResolverDefinition;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.google.common.collect.ImmutableSet;

public sealed interface InputResolverDefinition
    permits DefaultInputResolverDefinition, InputResolver {

  ImmutableSet<Integer> sources();

  QualifiedInputs resolutionTarget();

  boolean canFanout();
}

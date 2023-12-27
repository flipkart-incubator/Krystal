package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.vajram.facets.DefaultInputResolverDefinition;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.google.common.collect.ImmutableSet;

public sealed interface InputResolverDefinition
    permits InputResolver, DefaultInputResolverDefinition {

  ImmutableSet<String> sources();

  QualifiedInputs resolutionTarget();
}

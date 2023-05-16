package com.flipkart.krystal.vajram.inputs.resolution;

import com.flipkart.krystal.vajram.inputs.DefaultInputResolverDefinition;
import com.flipkart.krystal.vajram.inputs.QualifiedInputs;
import com.google.common.collect.ImmutableSet;

public sealed interface InputResolverDefinition
    permits InputResolver, DefaultInputResolverDefinition {

  ImmutableSet<String> sources();

  QualifiedInputs resolutionTarget();
}

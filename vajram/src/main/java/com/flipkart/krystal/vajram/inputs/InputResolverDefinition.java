package com.flipkart.krystal.vajram.inputs;

import com.google.common.collect.ImmutableSet;

public sealed interface InputResolverDefinition permits InputResolver, DefaultInputResolver {

  ImmutableSet<String> sources();

  QualifiedInputs resolutionTarget();
}

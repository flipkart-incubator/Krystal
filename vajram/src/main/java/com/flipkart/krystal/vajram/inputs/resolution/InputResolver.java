package com.flipkart.krystal.vajram.inputs.resolution;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.google.common.collect.ImmutableSet;

public non-sealed interface InputResolver extends InputResolverDefinition {
  DependencyCommand<Inputs> resolve(
      String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs);
}

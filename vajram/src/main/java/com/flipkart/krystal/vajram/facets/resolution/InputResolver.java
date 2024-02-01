package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.google.common.collect.ImmutableSet;

public non-sealed interface InputResolver extends InputResolverDefinition {
  DependencyCommand<Facets> resolve(
      String dependencyName, ImmutableSet<String> inputsToResolve, Facets facets);
}

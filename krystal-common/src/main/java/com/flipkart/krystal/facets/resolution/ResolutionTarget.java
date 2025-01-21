package com.flipkart.krystal.facets.resolution;

import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.RemoteInput;
import com.google.common.collect.ImmutableSet;

public record ResolutionTarget(Dependency dependency, ImmutableSet<RemoteInput> targetInputs) {

  public ResolutionTarget(Dependency dependency, RemoteInput targetInput) {
    this(dependency, ImmutableSet.of(targetInput));
  }
}

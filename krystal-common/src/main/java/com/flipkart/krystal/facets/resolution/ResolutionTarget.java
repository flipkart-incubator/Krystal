package com.flipkart.krystal.facets.resolution;

import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.InputMirror;
import com.google.common.collect.ImmutableSet;

public record ResolutionTarget(Dependency dependency, ImmutableSet<InputMirror> targetInputs) {

  public ResolutionTarget(Dependency dependency, InputMirror targetInput) {
    this(dependency, ImmutableSet.of(targetInput));
  }
}

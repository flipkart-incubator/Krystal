package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.google.common.collect.ImmutableSet;

public abstract class AbstractInputResolver implements InputResolver {

  private final ImmutableSet<String> sources;
  private final QualifiedInputs resolutionTarget;

  protected AbstractInputResolver(ImmutableSet<String> sources, QualifiedInputs resolutionTarget) {
    this.sources = sources;
    this.resolutionTarget = resolutionTarget;
  }

  @Override
  public ImmutableSet<String> sources() {
    return sources;
  }

  @Override
  public QualifiedInputs resolutionTarget() {
    return resolutionTarget;
  }
}

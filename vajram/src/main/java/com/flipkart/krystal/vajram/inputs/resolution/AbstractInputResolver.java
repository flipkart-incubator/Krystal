package com.flipkart.krystal.vajram.inputs.resolution;

import com.flipkart.krystal.vajram.inputs.QualifiedInputs;
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

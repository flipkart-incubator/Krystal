package com.flipkart.krystal.vajram.inputs;

import com.google.common.collect.ImmutableSet;

public abstract class AbstractInputResolver implements InputResolver {

  private final ImmutableSet<String> sources;
  private final QualifiedInputs resolutionTarget;

  public AbstractInputResolver(ImmutableSet<String> sources, QualifiedInputs resolutionTarget) {
    this.sources = sources;
    this.resolutionTarget = resolutionTarget;
  }

  public AbstractInputResolver(String source, QualifiedInputs resolutionTarget) {
    this.sources = ImmutableSet.of(source);
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

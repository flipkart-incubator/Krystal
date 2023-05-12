package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.Vajram;
import com.google.common.collect.ImmutableSet;

public abstract class AbstractInputResolver<V extends Vajram<?>> implements InputResolver {

  private final ImmutableSet<String> sources;
  private final QualifiedInputs resolutionTarget;

  protected AbstractInputResolver(ImmutableSet<String> sources, QualifiedInputs resolutionTarget) {
    this.sources = sources;
    this.resolutionTarget = resolutionTarget;
  }

  protected AbstractInputResolver(String source, QualifiedInputs resolutionTarget) {
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

package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.google.common.collect.ImmutableSet;

public abstract sealed class AbstractInputResolver implements InputResolver
    permits AbstractFanoutInputResolver, AbstractSimpleInputResolver, AbstractSingleInputResolver {

  private final ImmutableSet<Integer> sources;
  private final QualifiedInputs resolutionTarget;
  private int resolverId = -1;

  protected AbstractInputResolver(ImmutableSet<Integer> sources, QualifiedInputs resolutionTarget) {
    this.sources = sources;
    this.resolutionTarget = resolutionTarget;
  }

  @Override
  public ImmutableSet<Integer> sources() {
    return sources;
  }

  @Override
  public QualifiedInputs resolutionTarget() {
    return resolutionTarget;
  }
}

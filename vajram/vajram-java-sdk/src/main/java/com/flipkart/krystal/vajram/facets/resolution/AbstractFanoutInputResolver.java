package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.google.common.collect.ImmutableSet;

public abstract non-sealed class AbstractFanoutInputResolver extends AbstractInputResolver
    implements FanoutInputResolver {

  protected AbstractFanoutInputResolver(
      ImmutableSet<Integer> sources, QualifiedInputs resolutionTarget) {
    super(sources, resolutionTarget);
  }
}

package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.google.common.collect.ImmutableSet;

public abstract non-sealed class AbstractSingleInputResolver extends AbstractInputResolver
    implements SingleInputResolver {

  protected AbstractSingleInputResolver(
      ImmutableSet<Integer> sources, QualifiedInputs resolutionTarget) {
    super(sources, resolutionTarget);
  }
}

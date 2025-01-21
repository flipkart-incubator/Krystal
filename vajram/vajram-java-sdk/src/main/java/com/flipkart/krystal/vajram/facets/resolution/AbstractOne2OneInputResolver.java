package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolutionTarget;
import com.google.common.collect.ImmutableSet;

public abstract non-sealed class AbstractOne2OneInputResolver extends AbstractInputResolver
    implements SingleInputResolver {

  protected AbstractOne2OneInputResolver(
      ImmutableSet<Facet> sources, ResolutionTarget resolutionTarget) {
    super(sources, resolutionTarget, false);
  }
}

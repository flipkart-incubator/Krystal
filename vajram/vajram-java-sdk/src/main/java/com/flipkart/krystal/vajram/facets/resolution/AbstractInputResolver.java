package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolutionTarget;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.google.common.collect.ImmutableSet;

public abstract sealed class AbstractInputResolver implements InputResolver
    permits AbstractFanoutInputResolver, AbstractSimpleInputResolver, AbstractOne2OneInputResolver {

  private final ResolverDefinition definition;

  protected AbstractInputResolver(
      ImmutableSet<? extends Facet> sources, ResolutionTarget resolutionTarget, boolean canFanout) {
    this.definition = new ResolverDefinition(sources, resolutionTarget, canFanout);
  }

  @Override
  public ResolverDefinition definition() {
    return definition;
  }
}

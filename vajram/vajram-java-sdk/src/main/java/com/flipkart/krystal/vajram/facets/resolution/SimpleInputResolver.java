package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.vajram.facets.VajramDependencySpec;

/** A resolver which resolves exactly one input of a dependency. */
public sealed interface SimpleInputResolver extends InputResolver
    permits AbstractSimpleInputResolver {

  public VajramDependencySpec<?, ?, ?, ?> getDependency();

  public SimpleInputResolverSpec<?, ?, ?> getResolverSpec();
}

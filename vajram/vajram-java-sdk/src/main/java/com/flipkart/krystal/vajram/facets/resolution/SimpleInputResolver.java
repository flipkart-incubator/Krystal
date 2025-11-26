package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.vajram.facets.specs.DependencySpec;

/** A resolver which resolves exactly one input of a dependency. */
public sealed interface SimpleInputResolver extends InputResolver
    permits AbstractSimpleInputResolver {

  DependencySpec<?, ?, ?> getDependency();

  SimpleInputResolverSpec<?, ?, ?> getResolverSpec();
}

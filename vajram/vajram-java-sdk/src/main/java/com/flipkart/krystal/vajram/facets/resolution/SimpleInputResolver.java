package com.flipkart.krystal.vajram.facets.resolution;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.flipkart.krystal.vajram.facets.VajramDependencySpec;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import java.util.concurrent.atomic.LongAdder;

/** A resolver which resolves exactly one input of a dependency. */
public abstract sealed class SimpleInputResolver<S, T, CV extends Request<?>, DV extends Request<?>>
    extends AbstractInputResolver permits SimpleFanoutInputResolver, SimpleSingleInputResolver {
  public static final LongAdder TIME = new LongAdder();
  private final VajramDependencySpec<?, ?, CV, DV> dependency;

  private final SimpleInputResolverSpec<T, CV, DV> resolverSpec;

  SimpleInputResolver(
      VajramDependencySpec<?, ?, CV, DV> dependency,
      SimpleInputResolverSpec<T, CV, DV> resolverSpec) {
    super(
        resolverSpec.sourceInputs().stream().map(VajramFacetSpec::id).collect(toImmutableSet()),
        new QualifiedInputs(dependency.id(), resolverSpec.targetInput().name()));
    this.dependency = dependency;
    this.resolverSpec = resolverSpec;
  }

  public VajramDependencySpec<?, ?, ?, ?> getDependency() {
    return dependency;
  }

  public SimpleInputResolverSpec<?, ?, ?> getResolverSpec() {
    return resolverSpec;
  }
}

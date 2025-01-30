package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.resolution.ResolutionTarget;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import java.util.concurrent.atomic.LongAdder;

/** A resolver which resolves exactly one input of a dependency. */
public abstract sealed class AbstractSimpleInputResolver<
        S, T, CV extends Request, DV extends Request>
    extends AbstractInputResolver implements SimpleInputResolver
    permits SimpleFanoutInputResolver, SimpleOne2OneInputResolver {
  public static final LongAdder TIME = new LongAdder();
  private final DependencySpec<?, CV, DV> dependency;

  private final SimpleInputResolverSpec<T, CV, DV> resolverSpec;

  AbstractSimpleInputResolver(
      DependencySpec<?, CV, DV> dependency,
      SimpleInputResolverSpec<T, CV, DV> resolverSpec,
      boolean canFanout) {
    super(
        resolverSpec.sources(),
        new ResolutionTarget(dependency, resolverSpec.targetInput()),
        canFanout);
    this.dependency = dependency;
    this.resolverSpec = resolverSpec;
  }

  public DependencySpec<?, ?, ?> getDependency() {
    return dependency;
  }

  public SimpleInputResolverSpec<T, CV, DV> getResolverSpec() {
    return resolverSpec;
  }
}

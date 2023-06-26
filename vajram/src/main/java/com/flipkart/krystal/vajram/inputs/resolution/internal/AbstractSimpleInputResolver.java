package com.flipkart.krystal.vajram.inputs.resolution.internal;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Optional.ofNullable;

import com.flipkart.krystal.vajram.inputs.QualifiedInputs;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.AbstractInputResolver;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolverSpec;

public abstract class AbstractSimpleInputResolver extends AbstractInputResolver {

  private final VajramDependencyTypeSpec<?, ?, ?> dependency;
  private final InputResolverSpec<?, ?, ?, ?> resolverSpec;

  protected AbstractSimpleInputResolver(
      VajramDependencyTypeSpec<?, ?, ?> dependency, InputResolverSpec<?, ?, ?, ?> resolverSpec) {
    super(
        ofNullable(resolverSpec.getSourceInput()).stream()
            .map(VajramInputTypeSpec::name)
            .collect(toImmutableSet()),
        new QualifiedInputs(dependency.name(), resolverSpec.getTargetInput().name()));
    this.dependency = dependency;
    this.resolverSpec = resolverSpec;
  }

  public VajramDependencyTypeSpec<?, ?, ?> getDependency() {
    return dependency;
  }

  public InputResolverSpec<?, ?, ?, ?> getResolverSpec() {
    return resolverSpec;
  }
}

package com.flipkart.krystal.vajram.inputs.resolution;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Optional.ofNullable;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.QualifiedInputs;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.DoNotCall;
import java.util.List;
import java.util.Map;

/** A resolver which resolves exactly one input of a dependency. */
public final class SimpleInputResolver extends AbstractInputResolver {

  private final VajramDependencyTypeSpec<?, ?, ?> dependency;
  private final SimpleInputResolverSpec<?, ?, ?, ?> resolverSpec;

  SimpleInputResolver(
      VajramDependencyTypeSpec<?, ?, ?> dependency,
      SimpleInputResolverSpec<?, ?, ?, ?> resolverSpec) {
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

  public SimpleInputResolverSpec<?, ?, ?, ?> getResolverSpec() {
    return resolverSpec;
  }

  /**
   * @see InputResolverUtil#multiResolve(List, Map, Inputs)
   */
  @Override
  @Deprecated
  @DoNotCall("Simple Input resolvers should be InputResolverUtil#multiResolve")
  public DependencyCommand<Inputs> resolve(
      String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
    throw new UnsupportedOperationException(
        "This should not be called. See InputResolverUtil.multiResolve");
  }
}

package com.flipkart.krystal.vajram.inputs;

import static com.flipkart.krystal.data.ValueOrError.withValue;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;

@Builder
record StaticResolver(Object as, String dependencyName, String targetInputName)
    implements InputResolver {
  public static StaticResolver.StaticResolverBuilder staticResolve(String dep, String target) {
    return builder().dependencyName(dep).targetInputName(target);
  }

  @Override
  public ImmutableSet<String> sources() {
    return ImmutableSet.of();
  }

  @Override
  public QualifiedInputs resolutionTarget() {
    return new QualifiedInputs(dependencyName(), null, targetInputName());
  }

  @Override
  public DependencyCommand<Inputs> resolve(
      String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
    return DependencyCommand.singleExecuteWith(
        new Inputs(ImmutableMap.of(targetInputName(), withValue(as))));
  }
}

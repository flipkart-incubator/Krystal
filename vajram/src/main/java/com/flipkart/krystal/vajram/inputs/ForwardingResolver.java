package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.Vajram;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.function.Function;
import lombok.Builder;

@Builder
public record ForwardingResolver<S, T, CV extends Vajram<?>, DV extends Vajram<?>>(
    VajramInput<S, CV> using,
    Function<S, T> transformWith,
    VajramDependency<?, CV, DV> dependency,
    VajramInput<T, DV> targetInput)
    implements SingleInputResolver<S, T, Vajram<?>> {
  public static <T, CV extends Vajram<?>, DV extends Vajram<?>>
      ForwardingResolverBuilder<T, T, CV, DV> forwardResolve(
          VajramDependency<?, CV, DV> dependency, VajramInput<T, DV> depInput) {
    return ForwardingResolver.<T, T, CV, DV>builder()
        .transformWith(Functions.identity())
        .dependency(dependency)
        .targetInput(depInput);
  }

  @Override
  public ImmutableSet<String> sources() {
    return ImmutableSet.of(using.name());
  }

  @Override
  public QualifiedInputs resolutionTarget() {
    return new QualifiedInputs(dependency().name(), null, targetInput().name());
  }

  @Override
  public DependencyCommand<Inputs> resolve(
      String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
    return DependencyCommand.singleExecuteWith(
        new Inputs(
            ImmutableMap.of(targetInput().name(), inputs.getInputValue(this.using().name()))));
  }
}

package com.flipkart.krystal.vajram.inputs;

import static com.flipkart.krystal.vajram.inputs.DependencyCommand.singleExecuteWith;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
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
      ForwardingResolverBuilder<T, T, CV, DV> resolve(
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
    ValueOrError<Object> inputValue;
    if (using instanceof VajramDependency<?, ?, ?>) {
      inputValue = inputs.getDepValue(this.using().name()).values().values().iterator().next();
    } else {
      inputValue = inputs.getInputValue(this.using().name());
    }
    return singleExecuteWith(new Inputs(ImmutableMap.of(targetInput().name(), inputValue)));
  }
}
